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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

@JsonSubTypes({
    @JsonSubTypes.Type(name = DoiRequestDao.TYPE, value = DoiRequestDao.class),
    @JsonSubTypes.Type(name = PublishingRequestDao.TYPE, value = PublishingRequestDao.class),
    @JsonSubTypes.Type(name = GeneralSupportRequestDao.TYPE, value = GeneralSupportRequestDao.class),
    @JsonSubTypes.Type(name = GeneralSupportRequestDao.LEGACY_TYPE, value = GeneralSupportRequestDao.class),
    @JsonSubTypes.Type(name = UnpublishRequestDao.TYPE, value = UnpublishRequestDao.class),
    @JsonSubTypes.Type(name = FilesApprovalThesisDao.TYPE, value = FilesApprovalThesisDao.class)
})
public abstract class TicketDao extends Dao implements JoinWithResource {
    
    public static final String DOUBLE_QUOTES = "\"";
    public static final String EMPTY_STRING = "";
    public static final String TICKETS_INDEXING_TYPE = "Ticket";
    public static final String ALPHABETICALLY_ORDERED_FIRST_TICKET_TYPE = "b";
    public static final String ALPHABETICALLY_ORDERED_LAST_TICKET_TYPE = "e";
    private static final String TICKET_IDENTIFIER_FIELD_NAME = "ticketIdentifier";


    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdAt;

    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedAt;

    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;

    @JsonProperty(TICKET_IDENTIFIER_FIELD_NAME)
    private SortableIdentifier ticketIdentifier;

    @JsonProperty(RESOURCE_IDENTIFIER_FIELD)
    private SortableIdentifier resourceIdentifier;

    @JsonProperty(OWNER_FIELD)
    private User owner;
    
    protected TicketDao() {
        super();
    }
    
    protected TicketDao(TicketEntry ticketEntry) {
        super(ticketEntry);
        this.setIdentifier(ticketEntry.getIdentifier());
        this.createdAt = ticketEntry.getCreatedDate();
        this.modifiedAt = ticketEntry.getModifiedDate();
        this.customerId = ticketEntry.getCustomerId();
        this.ticketIdentifier = ticketEntry.getIdentifier();
        this.resourceIdentifier = ticketEntry.getResourceIdentifier();
        this.owner = ticketEntry.getOwner();
    }
    
    public final Optional<TicketDao> fetchTicket(DynamoDbClient client) {
        var request = GetItemRequest.builder()
                          .tableName(RESOURCES_TABLE_NAME)
                          .key(primaryKey())
                          .build();

        return attempt(() -> client.getItem(request))
                   .map(GetItemResponse::item).map(item -> DynamoEntry.parseAttributeValuesMap(item, TicketDao.class))
                   .toOptional();
    }

    public PutItemRequest createPutItemRequest() {
        var condition = new UpdateCaseButNotOwnerCondition(this);

        return PutItemRequest.builder()
                   .tableName(RESOURCES_TABLE_NAME)
                   .item(toDynamoFormat())
                   .conditionExpression(condition.getConditionExpression())
                   .expressionAttributeNames(condition.getExpressionAttributeNames())
                   .expressionAttributeValues(condition.getExpressionAttributeValues())
                   .build();
    }

    public TransactWriteItem toPutTransactionItem(String tableName) {
        var put = Put.builder().item(this.toDynamoFormat()).tableName(tableName).build();
        return TransactWriteItem.builder().put(put).build();
    }

    public Optional<TicketDao> fetchByResourceIdentifier(DynamoDbClient client) {
        var queryRequest = QueryRequest.builder()
                               .tableName(RESOURCES_TABLE_NAME)
                               .indexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                               .keyConditions(byResource(joinByResourceOrderedType()))
                               .build();

        var dynamoFormat = client.query(queryRequest)
                               .items()
                               .stream()
                               .collect(SingletonCollector.collectOrElse(null));
        return Optional.ofNullable(dynamoFormat)
                   .map(item -> DynamoEntry.parseAttributeValuesMap(item, this.getClass()));
    }

    public Stream<MessageDao> fetchTicketMessages(DynamoDbClient client) {
        var query = new FetchMessagesQuery(this);
        var request = QueryRequest.builder()
                          .tableName(RESOURCES_TABLE_NAME)
                          .indexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                          .keyConditionExpression(query.getKeyConditionExpression())
                          .filterExpression(query.getFilterExpression())
                          .expressionAttributeNames(query.getExpressionAttributeNames())
                          .expressionAttributeValues(query.getExpressionAttributeValues())
                          .build();
        var result = client.query(request)
                         .items()
                         .stream()
                         .map(item -> DynamoEntry.parseAttributeValuesMap(item, MessageDao.class))
                         .collect(Collectors.toList());
        return result.stream();
    }
    
    @Override
    public final String indexingType() {
        return TICKETS_INDEXING_TYPE;
    }
    
    @Override
    public void updateExistingEntry(DynamoDbClient client) {
        this.getData().setModifiedDate(Instant.now());
        var putItem = this.createPutItemRequest();
        try {
            client.putItem(putItem);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Instant modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    @Override
    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    public SortableIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }

    public void setTicketIdentifier(SortableIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
    }

    @Override
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    @Override
    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    protected static <T extends DynamoEntry> TransactWriteItem newPutTransactionItem(T data) {
        var put = Put.builder()
                      .item(data.toDynamoFormat())
                      .tableName(RESOURCES_TABLE_NAME)
                      .conditionExpression(KEY_NOT_EXISTS_CONDITION)
                      .expressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                      .build();
        return TransactWriteItem.builder().put(put).build();
    }
    
    protected TicketEntry getTicketEntry() {
        return (TicketEntry) getData();
    }

    
    private static class FetchMessagesQuery {
        
        private static final String FILTER_EXPRESSION = "#ticketIdentifier = :ticketIdentifier";
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
                "#ticketIdentifier", TICKET_IDENTIFIER_FIELD_NAME,
                "#JoinByResourcePartitionKey", BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME,
                "#JoinByResourceSortKey", BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME);
        }
        
        public Map<String, AttributeValue> getExpressionAttributeValues() {
            return Map.of(
                ":ticketIdentifier", AttributeValue.builder().s(ticketDao.getIdentifier().toString()).build(),
                ":PartitionKeyValue", AttributeValue.builder().s(ticketDao.getByCustomerAndResourcePartitionKey()).build(),
                ":SortKeyValuePrefix", AttributeValue.builder().s(MessageDao.joinByResourceOrderedContainedType()).build()
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
                    ":createdDate", AttributeValue.builder().s(dateAsString(entryUpdate.getCreatedDate())).build(),
                    ":customerId", AttributeValue.builder().s(entryUpdate.getCustomerId().toString()).build(),
                    ":identifier", AttributeValue.builder().s(entryUpdate.getIdentifier().toString()).build(),
                    ":modifiedDate", AttributeValue.builder().s(dateAsString(entryUpdate.getModifiedDate())).build(),
                    ":owner", AttributeValue.builder().s(entryUpdate.getOwner().toString()).build(),
                    ":resourceIdentifier", AttributeValue.builder().s(entryUpdate.getResourceIdentifier().toString()).build(),
                    ":version", AttributeValue.builder().s(dao.getVersion().toString()).build()
                );
            
            this.conditionExpression =
                "#createdDate = :createdDate "
                + "AND #customerId = :customerId "
                + "AND #identifier = :identifier "
                + "AND #modifiedDate <> :modifiedDate "
                + "AND #owner = :owner "
                + "AND #resourceIdentifier = :resourceIdentifier "
                + "AND #version <> :version ";
        }
        
        private String dateAsString(Instant date) {
            return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(date))
                       .map(dateStr -> dateStr.replace(DOUBLE_QUOTES, EMPTY_STRING))
                       .orElseThrow();
        }
    }
}
