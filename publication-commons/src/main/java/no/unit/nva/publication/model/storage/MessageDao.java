package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(MessageDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MessageDao extends Dao
    implements DynamoEntryByIdentifier, JoinWithResource {
    
    public static final String TYPE = "Message";
    private static final String JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX = "z";
    private Message data;
    
    public MessageDao() {
        super();
    }
    
    public MessageDao(Message message) {
        super();
        this.data = message;
    }
    
    public static MessageDao queryObject(UserInstance owner, SortableIdentifier identifier) {
        Message message = Message.builder()
                              .withOwner(owner.getUserIdentifier())
                              .withCustomerId(owner.getOrganizationUri())
                              .withIdentifier(identifier)
                              .build();
        return new MessageDao(message);
    }
    
    public static MessageDao listMessagesForCustomerAndStatus(URI customerId, TicketStatus ticketStatus) {
        Message message = Message.builder()
                              .withCustomerId(customerId)
                              .withStatus(ticketStatus)
                              .build();
        return new MessageDao(message);
    }
    
    public static MessageDao listMessagesAndResourcesForUser(UserInstance owner) {
        Message message = Message.builder()
                              .withCustomerId(owner.getOrganizationUri())
                              .withOwner(owner.getUserIdentifier())
                              .build();
        return new MessageDao(message);
    }
    
    public static String joinByResourceOrderedContainedType() {
        return JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + TYPE;
    }
    
    @Override
    public Message getData() {
        return data;
    }
    
    @Override
    public void setData(Entity data) {
        this.data = (Message) data;
    }
    
    @Override
    public String indexingType() {
        return TYPE;
    }
    
    @Override
    public URI getCustomerId() {
        return data.getCustomerId();
    }
    
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {
        
        var uniqueIdentifierEntry = new IdentifierEntry(this.getIdentifier().toString());
        var messageEntry = transactionItem(this);
        var identityEntry = transactionItem(uniqueIdentifierEntry);
        return new TransactWriteItemsRequest()
                   .withTransactItems(messageEntry, identityEntry);
    }
    
    @Override
    protected String getOwner() {
        return data.getOwner();
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getData());
    }
    
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageDao)) {
            return false;
        }
        MessageDao that = (MessageDao) o;
        return Objects.equals(getData(), that.getData());
    }
    
    @Override
    public String joinByResourceOrderedType() {
        return joinByResourceOrderedContainedType();
    }
    
    @Override
    public SortableIdentifier getResourceIdentifier() {
        return data.getResourceIdentifier();
    }
    
    private static TransactWriteItem transactionItem(DynamoEntry dynamoEntry) {
        var put = new Put()
                      .withTableName(RESOURCES_TABLE_NAME)
                      .withItem(dynamoEntry.toDynamoFormat());
        
        return new TransactWriteItem().withPut(put);
    }
}
