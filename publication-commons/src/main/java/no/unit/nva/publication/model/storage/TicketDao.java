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
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;

@JsonSubTypes({
    @JsonSubTypes.Type(name = DoiRequestDao.TYPE, value = DoiRequestDao.class),
    @JsonSubTypes.Type(name = MessageDao.TYPE, value = MessageDao.class),
    @JsonSubTypes.Type(name = PublishingRequestDao.TYPE, value = PublishingRequestDao.class),
})
public abstract class TicketDao extends Dao implements JoinWithResource {
    
    public static final String DOUBLE_QUOTES = "\"";
    public static final String EMPTY_STRING = "";
    
    protected TicketDao() {
        super();
    }
    
    public static <T extends TicketEntry> TicketDao queryObject(TicketEntry ticketEntry, Class<T> ticketType) {
        if (PublishingRequestCase.class.equals(ticketType)) {
            return (PublishingRequestDao) ticketEntry.toDao();
        }
        if (DoiRequest.class.equals(ticketType)) {
            return (DoiRequestDao) ticketEntry.toDao();
        }
        throw new UnsupportedOperationException();
    }
    
    public static <T extends TicketEntry> QueryRequest queryByCustomerAndResource(URI customerId,
                                                                                  SortableIdentifier resourceIdentifier,
                                                                                  Class<T> ticketType) {
        var queryObject = TicketDao.createQueryObject(resourceIdentifier, customerId, ticketType);
        return new QueryRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
            .withKeyConditions(queryObject.byResource(queryObject.joinByResourceOrderedType()));
    }
    
    public abstract Optional<TicketDao> fetchItem(AmazonDynamoDB client);
    
    public PutItemRequest createPutItemRequest() {
        var condition = new UpdateCaseButNotOwnerCondition((TicketEntry) this.getData());
        
        return new PutItemRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withItem(toDynamoFormat())
            .withConditionExpression(condition.getConditionExpression())
            .withExpressionAttributeNames(condition.getExpressionAttributeNames())
            .withExpressionAttributeValues(condition.getExpressionAttributeValues());
    }
    
    protected static <T extends DynamoEntry> TransactWriteItem newPutTransactionItem(T data) {
        Put put = new Put()
            .withItem(data.toDynamoFormat())
            .withTableName(RESOURCES_TABLE_NAME)
            .withConditionExpression(KEY_NOT_EXISTS_CONDITION)
            .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES);
        return new TransactWriteItem().withPut(put);
    }
    
    protected <T extends TicketDao> Optional<TicketDao> fetchItemWithClient(AmazonDynamoDB client,
                                                                            Class<T> ticketDaoType) {
        var request = new GetItemRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withKey(primaryKey());
        var queryResult = client.getItem(request);
        return attempt(queryResult::getItem)
            .map(item -> DynamoEntry.parseAttributeValuesMap(item, ticketDaoType))
            .map(TicketDao.class::cast)
            .toOptional();
    }
    
    private static <T extends TicketEntry> TicketDao createQueryObject(SortableIdentifier resourceIdentifier,
                                                                       URI customerId,
                                                                       Class<T> ticketType) {
        if (DoiRequest.class.equals(ticketType)) {
            var doiRequest = DoiRequest.builder()
                .withResourceIdentifier(resourceIdentifier)
                .withCustomerId(customerId)
                .build();
            return new DoiRequestDao(doiRequest);
        }
        if (PublishingRequestCase.class.equals(ticketType)) {
            var request = new PublishingRequestCase();
            request.setResourceIdentifier(resourceIdentifier);
            request.setCustomerId(customerId);
            return new PublishingRequestDao(request);
        }
        throw new UnsupportedOperationException();
    }
    
    private static class UpdateCaseButNotOwnerCondition {
        
        private String conditionExpression;
        private Map<String, String> expressionAttributeNames;
        private Map<String, AttributeValue> expressionAttributeValues;
        
        public UpdateCaseButNotOwnerCondition(TicketEntry entryUpdate) {
            createCondition(entryUpdate);
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
        
        private void createCondition(TicketEntry entryUpdate) {
            
            this.expressionAttributeNames = Map.of(
                "#data", CONTAINED_DATA_FIELD_NAME,
                "#createdDate", CREATED_DATE_FIELD,
                "#customerId", CUSTOMER_ID_FIELD,
                "#identifier", IDENTIFIER_FIELD,
                "#modifiedDate", MODIFIED_DATE_FIELD,
                "#owner", OWNER_FIELD,
                "#resourceIdentifier", RESOURCE_IDENTIFIER_FIELD,
                "#version", Entity.VERSION
            );
            
            this.expressionAttributeValues =
                Map.of(
                    ":createdDate", new AttributeValue(dateAsString(entryUpdate.getCreatedDate())),
                    ":customerId", new AttributeValue(entryUpdate.getCustomerId().toString()),
                    ":identifier", new AttributeValue(entryUpdate.getIdentifier().toString()),
                    ":modifiedDate", new AttributeValue(dateAsString(entryUpdate.getModifiedDate())),
                    ":owner", new AttributeValue(entryUpdate.getOwner()),
                    ":resourceIdentifier", new AttributeValue(entryUpdate.getResourceIdentifier().toString()),
                    ":version", new AttributeValue(entryUpdate.getVersion().toString())
                );
            
            this.conditionExpression =
                "#data.#createdDate = :createdDate "
                + "AND #data.#customerId = :customerId "
                + "AND #data.#identifier = :identifier "
                + "AND #data.#modifiedDate <> :modifiedDate "
                + "AND #data.#owner = :owner "
                + "AND #data.#resourceIdentifier = :resourceIdentifier "
                + "AND #data.#version <> :version ";
        }
        
        private String dateAsString(Instant date) {
            return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(date))
                .map(dateStr -> dateStr.replace(DOUBLE_QUOTES, EMPTY_STRING))
                .orElseThrow();
        }
    }
}
