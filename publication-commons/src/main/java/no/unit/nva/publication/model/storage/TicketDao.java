package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.RESOURCE_IDENTIFIER_FIELD;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.model.business.TicketEntry;
import nva.commons.core.SingletonCollector;

@JsonSubTypes({
    @JsonSubTypes.Type(name = DoiRequestDao.TYPE, value = DoiRequestDao.class),
    @JsonSubTypes.Type(name = PublishingRequestDao.TYPE, value = PublishingRequestDao.class),
    @JsonSubTypes.Type(name = GeneralSupportRequestDao.TYPE, value = GeneralSupportRequestDao.class)
})
public abstract class TicketDao extends Dao implements JoinWithResource {
    
    public static final String DOUBLE_QUOTES = "\"";
    public static final String EMPTY_STRING = "";
    private static final String TICKET_IDENTIFIER_FIELD_NAME = "ticketIdentifier";
    public static final String TICKETS_INDEXING_TYPE = "Ticket";
    
    protected TicketDao() {
        super();
    }
    
    protected TicketDao(TicketEntry ticketEntry) {
        super(ticketEntry);
    }
    
    public final Optional<TicketDao> fetchTicket(AmazonDynamoDB client) {
        var request = new GetItemRequest()
                          .withTableName(RESOURCES_TABLE_NAME)
                          .withKey(primaryKey());
    
        return attempt(() -> client.getItem(request))
                   .map(GetItemResult::getItem).map(item -> DynamoEntry.parseAttributeValuesMap(item, TicketDao.class))
                   .toOptional();
    }
    
    public PutItemRequest createPutItemRequest() {
        var condition = new UpdateCaseButNotOwnerCondition(this);
        
        return new PutItemRequest()
                   .withTableName(RESOURCES_TABLE_NAME)
                   .withItem(toDynamoFormat())
                   .withConditionExpression(condition.getConditionExpression())
                   .withExpressionAttributeNames(condition.getExpressionAttributeNames())
                   .withExpressionAttributeValues(condition.getExpressionAttributeValues());
    }
    
    public Optional<TicketDao> fetchByResourceIdentifier(AmazonDynamoDB client) {
        QueryRequest queryRequest = new QueryRequest()
                                        .withTableName(RESOURCES_TABLE_NAME)
                                        .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                                        .withKeyConditions(byResource(joinByResourceOrderedType()));
    
        var dynamoFormat = client.query(queryRequest)
                               .getItems()
                               .stream()
                               .collect(SingletonCollector.collectOrElse(null));
        return Optional.ofNullable(dynamoFormat)
                   .map(item -> DynamoEntry.parseAttributeValuesMap(item, this.getClass()));
    }
    
    public Stream<MessageDao> fetchTicketMessages(AmazonDynamoDB client) {
        var query = new FetchMessagesQuery(this);
        var request = new QueryRequest()
                          .withTableName(RESOURCES_TABLE_NAME)
                          .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                          .withKeyConditionExpression(query.getKeyConditionExpression())
                          .withFilterExpression(query.getFilterExpression())
                          .withExpressionAttributeNames(query.getExpressionAttributeNames())
                          .withExpressionAttributeValues(query.getExpressionAttributeValues());
        var result = client.query(request)
                         .getItems()
                         .stream()
                         .map(item -> DynamoEntry.parseAttributeValuesMap(item, MessageDao.class))
                         .collect(Collectors.toList());
        return result.stream();
    }
    
    protected static <T extends DynamoEntry> TransactWriteItem newPutTransactionItem(T data) {
        Put put = new Put()
                      .withItem(data.toDynamoFormat())
                      .withTableName(RESOURCES_TABLE_NAME)
                      .withConditionExpression(KEY_NOT_EXISTS_CONDITION)
                      .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES);
        return new TransactWriteItem().withPut(put);
    }
    
    @Override
    public final String indexingType() {
        return TICKETS_INDEXING_TYPE;
    }
    
    protected TicketEntry getTicketEntry() {
        return (TicketEntry) getData();
    }
    
    private static class FetchMessagesQuery {
        
        private static final String FILTER_EXPRESSION = "#data.#ticketIdentifier = :ticketIdentifier";
        private static final String KEY_CONDITION_EXPRESSION =
            "#JoinByResourcePartitionKey = :PartitionKeyValue "
            + "AND begins_with(#JoinByResourceSortKey,:SortKeyValuePrefix)";
        private final TicketDao ticketDao;
        
        public FetchMessagesQuery(TicketDao ticketDao) {
            this.ticketDao = ticketDao;
        }
        
        public String getKeyConditionExpression() {
            return KEY_CONDITION_EXPRESSION;
        }
        
        public String getFilterExpression() {
            return FILTER_EXPRESSION;
        }
        
        public Map<String, String> getExpressionAttributeNames() {
            return Map.of(
                "#data", CONTAINED_DATA_FIELD_NAME,
                "#ticketIdentifier", TICKET_IDENTIFIER_FIELD_NAME,
                "#JoinByResourcePartitionKey", BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME,
                "#JoinByResourceSortKey", BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME);
        }
        
        public Map<String, AttributeValue> getExpressionAttributeValues() {
            return Map.of(
                ":ticketIdentifier", new AttributeValue(ticketDao.getIdentifier().toString()),
                ":PartitionKeyValue", new AttributeValue(ticketDao.getByCustomerAndResourcePartitionKey()),
                ":SortKeyValuePrefix", new AttributeValue(MessageDao.joinByResourceOrderedContainedType())
            );
        }
    }
    
    private static class UpdateCaseButNotOwnerCondition {
        
        private String conditionExpression;
        private Map<String, String> expressionAttributeNames;
        private Map<String, AttributeValue> expressionAttributeValues;
        
        public UpdateCaseButNotOwnerCondition(TicketDao dao) {
            createCondition(dao);
        }
        
        public String getConditionExpression() {
            return conditionExpression;
        }
        
        public Map<String, String> getExpressionAttributeNames() {
            return expressionAttributeNames;
        }
        
        public Map<String, AttributeValue> getExpressionAttributeValues() {
            return expressionAttributeValues;
        }
        
        private void createCondition(TicketDao dao) {
            var entryUpdate = (TicketEntry) dao.getData();
            this.expressionAttributeNames = Map.of(
                "#data", CONTAINED_DATA_FIELD_NAME,
                "#createdDate", CREATED_DATE_FIELD,
                "#customerId", CUSTOMER_ID_FIELD,
                "#identifier", IDENTIFIER_FIELD,
                "#modifiedDate", MODIFIED_DATE_FIELD,
                "#owner", OWNER_FIELD,
                "#resourceIdentifier", RESOURCE_IDENTIFIER_FIELD,
                "#version", VERSION_FIELD
            );
            
            this.expressionAttributeValues =
                Map.of(
                    ":createdDate", new AttributeValue(dateAsString(entryUpdate.getCreatedDate())),
                    ":customerId", new AttributeValue(entryUpdate.getCustomerId().toString()),
                    ":identifier", new AttributeValue(entryUpdate.getIdentifier().toString()),
                    ":modifiedDate", new AttributeValue(dateAsString(entryUpdate.getModifiedDate())),
                    ":owner", new AttributeValue(entryUpdate.getOwner().toString()),
                    ":resourceIdentifier", new AttributeValue(entryUpdate.getResourceIdentifier().toString()),
                    ":version", new AttributeValue(dao.getVersion().toString())
                );
            
            this.conditionExpression =
                "#data.#createdDate = :createdDate "
                + "AND #data.#customerId = :customerId "
                + "AND #data.#identifier = :identifier "
                + "AND #data.#modifiedDate <> :modifiedDate "
                + "AND #data.#owner = :owner "
                + "AND #data.#resourceIdentifier = :resourceIdentifier "
                + "AND #version <> :version ";
        }
        
        private String dateAsString(Instant date) {
            return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(date))
                       .map(dateStr -> dateStr.replace(DOUBLE_QUOTES, EMPTY_STRING))
                       .orElseThrow();
        }
    }
}
