package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.Entity.nextVersion;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.MessageDao;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class Message implements TicketEntry,
                                JsonSerializable {
    
    public static final String TYPE = "Message";
    public static final String SUPPORT_SERVICE_RECIPIENT = "SupportService";
    
    @JsonProperty("identifier")
    private SortableIdentifier identifier;
    @JsonProperty("owner")
    private String owner;
    @JsonProperty("customerId")
    private URI customerId;
    @JsonProperty("status")
    private MessageStatus status;
    @JsonProperty("sender")
    private String sender;
    @JsonProperty("resourceIdentifier")
    private SortableIdentifier resourceIdentifier;
    @JsonProperty("text")
    private String text;
    //TODO: remove alias after migration
    @JsonAlias("createdTime")
    @JsonProperty("createdDate")
    private Instant createdDate;
    //TODO: remove alias after migration
    @JsonAlias("modifiedTime")
    @JsonProperty("modifiedDate")
    private Instant modifiedDate;
    @JsonProperty("resourceTitle")
    private String resourceTitle;
    @JsonProperty("messageType")
    private MessageType messageType;
    @JsonProperty("rowVersion")
    private UUID rowVersion;
    
    @JacocoGenerated
    public Message() {
    }
    
    public static MessageBuilder builder() {
        return new MessageBuilder();
    }
    
    public static Message create(UserInstance sender,
                                 Publication publication,
                                 String messageText,
                                 SortableIdentifier messageIdentifier,
                                 Clock clock,
                                 MessageType messageType) {
        return buildMessage(sender, publication, messageText, messageIdentifier, clock)
            .withMessageType(messageType)
            .build();
    }
    
    @Deprecated
    public static Message supportMessage(UserInstance sender,
                                         Publication publication,
                                         String messageText,
                                         Clock clock) {
        return create(sender, publication, messageText, null, clock, MessageType.SUPPORT);
    }
    
    @JsonProperty("recipient")
    public String getRecipient() {
        return owner.equals(sender) ? SUPPORT_SERVICE_RECIPIENT : owner;
    }
    
    public void setRecipient(String recipient) {
        // DO NOTHING
    }
    
    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }
    
    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    public Message markAsRead(Clock clock) {
        return this.copy()
            .withStatus(MessageStatus.READ)
            .withModifiedTime(clock.instant())
            .withRowVersion(UUID.randomUUID())
            .build();
    }
    
    public MessageType getMessageType() {
        return messageType;
    }
    
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }
    
    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }
    
    @Override
    public Publication toPublication() {
        throw new UnsupportedOperationException();
    }
    
    @JacocoGenerated
    @Override
    public UUID getVersion() {
        return this.rowVersion;
    }
    
    @Override
    @JacocoGenerated
    public void setVersion(UUID rowVersion) {
        this.rowVersion = rowVersion;
    }
    
    @Override
    public String getType() {
        return Message.TYPE;
    }
    
    //TODO: cover this method when Message is not a ticket any more.
    @JacocoGenerated
    @Override
    public MessageDao toDao() {
        return new MessageDao(this);
    }
    
    @Override
    public URI getCustomerId() {
        return customerId;
    }
    
    @Override
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }
    
    public MessageStatus getStatus() {
        return status;
    }
    
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    @Override
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }
    
    //TODO: remove method or cover when Message is not a Ticket anymore.
    @JacocoGenerated
    @Override
    public void validateRequirements(Publication publication) {
    
    }
    
    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    public String getResourceTitle() {
        return resourceTitle;
    }
    
    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }
    
    @Override
    public String getStatusString() {
        return status.toString();
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getOwner(), getCustomerId(), getStatus(), getSender(),
            getResourceIdentifier(),
            getText(), getCreatedDate(), getModifiedDate(), getResourceTitle(), getMessageType());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message)) {
            return false;
        }
        Message message = (Message) o;
        
        return Objects.equals(getIdentifier(), message.getIdentifier())
               && Objects.equals(getOwner(), message.getOwner())
               && Objects.equals(getCustomerId(), message.getCustomerId())
               && getStatus() == message.getStatus()
               && Objects.equals(getSender(), message.getSender())
               && Objects.equals(getResourceIdentifier(), message.getResourceIdentifier())
               && Objects.equals(getText(), message.getText())
               && Objects.equals(getCreatedDate(), message.getCreatedDate())
               && Objects.equals(getModifiedDate(), message.getModifiedDate())
               && Objects.equals(getResourceTitle(), message.getResourceTitle())
               && getMessageType() == message.getMessageType();
    }
    
    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
    
    public MessageBuilder copy() {
        return Message.builder()
            .withCreatedTime(this.getCreatedDate())
            .withCustomerId(this.getCustomerId())
            .withIdentifier(this.getIdentifier())
            .withMessageType(this.getMessageType())
            .withResourceIdentifier(getResourceIdentifier())
            .withStatus(this.getStatus())
            .withOwner(this.getOwner())
            .withSender(this.getSender())
            .withText(this.getText())
            .withResourceTitle(this.getResourceTitle())
            .withModifiedTime(this.getModifiedDate())
            .withRowVersion(this.getVersion());
    }
    
    private static MessageBuilder buildMessage(UserInstance sender, Publication publication,
                                               String messageText, SortableIdentifier messageIdentifier,
                                               Clock clock) {
        
        var now = clock.instant();
        return Message.builder()
            .withStatus(MessageStatus.UNREAD)
            .withResourceIdentifier(publication.getIdentifier())
            .withCustomerId(sender.getOrganizationUri())
            .withText(messageText)
            .withSender(sender.getUserIdentifier())
            .withOwner(publication.getResourceOwner().getOwner())
            .withResourceTitle(extractTitle(publication))
            .withCreatedTime(now)
            .withModifiedTime(now)
            .withIdentifier(messageIdentifier)
            .withRowVersion(nextVersion());
    }
    
    private static String extractTitle(Publication publication) {
        return Optional.of(publication)
            .map(Publication::getEntityDescription)
            .map(EntityDescription::getMainTitle)
            .orElse(null);
    }
}
