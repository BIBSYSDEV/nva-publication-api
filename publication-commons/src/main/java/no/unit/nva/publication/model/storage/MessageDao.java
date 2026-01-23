package no.unit.nva.publication.model.storage;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

@JsonTypeName(MessageDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MessageDao extends Dao
    implements DynamoEntryByIdentifier, JoinWithResource {
    
    public static final String TYPE = "Message";
    private static final String JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX = "z";

    private static final String TICKET_IDENTIFIER_FIELD_NAME = "ticketIdentifier";

    @JsonProperty(TICKET_IDENTIFIER_FIELD_NAME)
    private SortableIdentifier ticketIdentifier;

    public MessageDao() {
        super();
    }
    
    public MessageDao(Message message) {
        super(message);
        this.setIdentifier(message.getIdentifier());
        this.ticketIdentifier = message.getTicketIdentifier();
    }
    
    public static MessageDao queryObject(UserInstance owner, SortableIdentifier identifier) {
        Message message = Message.builder()
                              .withOwner(owner.getUser())
                              .withCustomerId(owner.getCustomerId())
                              .withIdentifier(identifier)
                              .build();
        return new MessageDao(message);
    }
    
    public static MessageDao listMessagesAndResourcesForUser(UserInstance owner) {
        Message message = Message.builder()
                              .withCustomerId(owner.getCustomerId())
                              .withOwner(owner.getUser())
                              .build();
        return new MessageDao(message);
    }
    
    public static String joinByResourceOrderedContainedType() {
        return JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + TYPE;
    }
    
    @Override
    public String indexingType() {
        return TYPE;
    }
    
    @Override
    public URI getCustomerId() {
        return getData().getCustomerId();
    }
    
    @Override
    public TransactWriteItemsRequest createInsertionTransactionRequest() {

        var uniqueIdentifierEntry = new IdentifierEntry(this.getIdentifier().toString());
        var messageEntry = transactionItem(this);
        var identityEntry = transactionItem(uniqueIdentifierEntry);
        return TransactWriteItemsRequest.builder()
                   .transactItems(messageEntry, identityEntry)
                   .build();
    }

    @JacocoGenerated
    @Override
    public void updateExistingEntry(DynamoDbClient client) {
        try {
            client.putItem(this.createPutItemRequest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PutItemRequest createPutItemRequest() {
        return PutItemRequest.builder()
                   .tableName(RESOURCES_TABLE_NAME)
                   .item(toDynamoFormat())
                   .build();
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

    public SortableIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }

    public void setTicketIdentifier(SortableIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
    }

    @Override
    protected User getOwner() {
        return getData().getOwner();
    }
    
    @JsonIgnore
    public Message getMessage() {
        return (Message) getData();
    }
    
    @Override
    public String joinByResourceOrderedType() {
        return joinByResourceOrderedContainedType();
    }

    @Override
    public SortableIdentifier getResourceIdentifier() {
        return ((Message) getData()).getResourceIdentifier();
    }

    private static TransactWriteItem transactionItem(DynamoEntry dynamoEntry) {
        var put = Put.builder()
                      .tableName(RESOURCES_TABLE_NAME)
                      .item(dynamoEntry.toDynamoFormat())
                      .build();

        return TransactWriteItem.builder().put(put).build();
    }
}
