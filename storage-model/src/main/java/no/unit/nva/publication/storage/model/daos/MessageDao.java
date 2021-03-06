package no.unit.nva.publication.storage.model.daos;

import static no.unit.nva.publication.storage.model.DatabaseConstants.KEY_FIELDS_DELIMITER;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.MessageStatus;
import no.unit.nva.publication.storage.model.UserInstance;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(MessageDao.TYPE)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MessageDao extends Dao<Message>
    implements JoinWithResource {
    
    public static final String TYPE = "Message";
    private static final String JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX = "c";
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

    public static MessageDao listMessagesForCustomerAndStatus(URI customerId, MessageStatus messageStatus) {

        Message message = Message.builder()
                              .withCustomerId(customerId)
                              .withStatus(messageStatus)
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

    @Override
    public Message getData() {
        return data;
    }

    @Override
    public void setData(Message data) {
        this.data = data;
    }
    
    @Override
    public String getType() {
        return TYPE;
    }
    
    @Override
    public URI getCustomerId() {
        return data.getCustomerId();
    }
    
    @Override
    public SortableIdentifier getIdentifier() {
        return data.getIdentifier();
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
    
    public static String joinByResourceOrderedContainedType() {
        return JOIN_BY_RESOURCE_INDEX_ORDER_PREFIX + KEY_FIELDS_DELIMITER + TYPE;
    }
    
    @Override
    public String joinByResourceOrderedType() {
        return joinByResourceOrderedContainedType();
    }
    
    @Override
    public SortableIdentifier getResourceIdentifier() {
        return data.getResourceIdentifier();
    }
}
