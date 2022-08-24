package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(PublishingRequestDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PublishingRequestDao extends TicketDao implements JoinWithResource, JsonSerializable {
    
    public static final String BY_RESOURCE_INDEX_ORDER_PREFIX = "d";
    public static final String TYPE = "PublishingRequestCase";
    private PublishingRequestCase data;
    
    @JacocoGenerated
    public PublishingRequestDao() {
        super();
    }
    
    public PublishingRequestDao(TicketEntry data) {
        super();
        this.data = (PublishingRequestCase) data;
    }
    
    public static QueryRequest queryPublishingRequestByResource(URI customerId,
                                                                SortableIdentifier resourceIdentifier) {
        var queryObject = PublishingRequestCase.createQueryObject(resourceIdentifier, customerId);
        var dao = new PublishingRequestDao(queryObject);
        
        return new QueryRequest()
            .withTableName(RESOURCES_TABLE_NAME)
            .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
            .withKeyConditions(dao.byResource(dao.joinByResourceOrderedType()));
    }
    
    public static PublishingRequestDao queryObject(PublishingRequestCase queryObject) {
        return new PublishingRequestDao(queryObject);
    }
    
    public static PublishingRequestDao queryByCustomerAndResourceIdentifier(UserInstance resourceOwner,
                                                                            SortableIdentifier resourceIdentifier) {
        var queryObject =
            PublishingRequestCase.createQueryObject(resourceOwner, resourceIdentifier, null);
        return new PublishingRequestDao(queryObject);
    }
    
    @Override
    public Optional<TicketDao> fetchItem(AmazonDynamoDB client) {
        return fetchItemWithClient(client);
    }
    
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        var publicationRequestEntry = createPublishingRequestInsertionEntry(data);
        var identifierEntry = createUniqueIdentifierEntry(data);
        var publishingRequestUniquenessEntry = createPublishingRequestUniquenessEntry(data);
        return new TransactWriteItemsRequest()
            .withTransactItems(
                identifierEntry,
                publicationRequestEntry,
                publishingRequestUniquenessEntry);
    }
    
    private TransactWriteItem createPublishingRequestUniquenessEntry(PublishingRequestCase publishingRequest) {
        var publishingRequestUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        return newPutTransactionItem(publishingRequestUniquenessEntry);
    }
    
    private TransactWriteItem createPublishingRequestInsertionEntry(PublishingRequestCase publicationRequest) {
        var dynamoEntry = new PublishingRequestDao(publicationRequest);
        return newPutTransactionItem(dynamoEntry);
    }
    
    private TransactWriteItem createUniqueIdentifierEntry(PublishingRequestCase publicationRequest) {
        var identifierEntry = new IdentifierEntry(publicationRequest.getIdentifier().toString());
        return newPutTransactionItem(identifierEntry);
    }
    
    public static String getContainedType() {
        return PublishingRequestCase.TYPE;
    }
    
    @JsonIgnore
    public static String joinByResourceContainedOrderedType() {
        return BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + PublishingRequestCase.TYPE;
    }
    
    @Override
    public PublishingRequestCase getData() {
        return data;
    }
    
    @Override
    public void setData(Entity data) {
        this.data = (PublishingRequestCase) data;
    }
    
    @Override
    public String getType() {
        return getContainedType();
    }
    
    @Override
    public URI getCustomerId() {
        return data.getCustomerId();
    }
    
    @Override
    protected String getOwner() {
        return data.getOwner();
    }
    
    @Override
    public String joinByResourceOrderedType() {
        return joinByResourceContainedOrderedType();
    }
    
    @Override
    public SortableIdentifier getResourceIdentifier() {
        return data.getResourceIdentifier();
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getData());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestDao)) {
            return false;
        }
        PublishingRequestDao that = (PublishingRequestDao) o;
        return Objects.equals(getData(), that.getData());
    }
    
    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
}

