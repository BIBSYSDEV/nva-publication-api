package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import java.net.URI;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;

@JsonSubTypes({
    @JsonSubTypes.Type(name = DoiRequestDao.TYPE, value = DoiRequestDao.class),
    @JsonSubTypes.Type(name = MessageDao.TYPE, value = MessageDao.class),
    @JsonSubTypes.Type(name = PublishingRequestDao.TYPE, value = PublishingRequestDao.class),
})
public abstract class TicketDao extends Dao implements JoinWithResource {
    
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
    
    protected static <T extends DynamoEntry> TransactWriteItem newPutTransactionItem(T data) {
        Put put = new Put()
            .withItem(data.toDynamoFormat())
            .withTableName(RESOURCES_TABLE_NAME)
            .withConditionExpression(KEY_NOT_EXISTS_CONDITION)
            .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES);
        return new TransactWriteItem().withPut(put);
    }
    
    protected <T extends TicketDao> Optional<TicketDao> fetchItem(AmazonDynamoDB client, Class<T> ticketDaoType) {
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
}
