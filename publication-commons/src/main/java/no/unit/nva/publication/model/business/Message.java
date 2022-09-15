package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.MessageDao;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
public class Message implements Entity, JsonSerializable {
    
    public static final String TYPE = "Message";
    public static final User SUPPORT_SERVICE_CORRESPONDENT = new User("SupportService");
    
    @JsonProperty("identifier")
    private SortableIdentifier identifier;
    @JsonProperty("owner")
    private User owner;
    @JsonProperty("customerId")
    private URI customerId;
    @JsonProperty("status")
    private MessageStatus status;
    @JsonProperty("sender")
    private User sender;
    @JsonProperty("resourceIdentifier")
    private SortableIdentifier resourceIdentifier;
    @JsonProperty("ticketIdentifier")
    private SortableIdentifier ticketIdentifier;
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
    
    @JacocoGenerated
    public Message() {
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Message create(TicketEntry ticket,
                                 UserInstance sender,
                                 String message) {
        var now = Instant.now();
        return builder()
                   .withCreatedDate(now)
                   .withModifiedDate(now)
                   .withCustomerId(ticket.getCustomerId())
                   .withOwner(ticket.getOwner())
                   .withMessageType(calculateMessageType(ticket))
                   .withIdentifier(SortableIdentifier.next())
                   .withText(message)
                   .withSender(sender.getUser())
                   .withResourceTitle("NOT_USED")
                   .withResourceIdentifier(ticket.getResourceIdentifier())
                   .withTicketIdentifier(ticket.getIdentifier())
                   .withStatus(MessageStatus.UNREAD)
                   .build();
    }
    
    //TODO: remove when ticket service is in place and doirequest and publishing-request services are deleted
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
    
    public MessageStatus getStatus() {
        return status;
    }
    
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    
    public SortableIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }
    
    public void setTicketIdentifier(SortableIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
    }
    
    @JsonProperty("recipient")
    public User getRecipient() {
        return owner.equals(sender) ? SUPPORT_SERVICE_CORRESPONDENT : owner;
    }
    
    public void setRecipient(String recipient) {
        // DO NOTHING
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getOwner(), getCustomerId(), getStatus(), getSender(),
            getResourceIdentifier(), getTicketIdentifier(), getText(), getCreatedDate(), getModifiedDate(),
            getResourceTitle(), getMessageType());
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
               && Objects.equals(getTicketIdentifier(), message.getTicketIdentifier())
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
    
    public Message markAsRead(Clock clock) {
        var copy = this.copy();
        copy.setStatus(MessageStatus.READ);
        copy.setModifiedDate(clock.instant());
        
        return copy;
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
    
    @Override
    public String getType() {
        return Message.TYPE;
    }
    
    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }
    
    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    @Override
    public User getOwner() {
        return owner;
    }
    
    @Override
    public URI getCustomerId() {
        return customerId;
    }
    
    @Override
    public Dao toDao() {
        return new MessageDao(this);
    }
    
    @Override
    public String getStatusString() {
        return status.toString();
    }
    
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    public User getSender() {
        return sender;
    }
    
    public void setSender(User sender) {
        this.sender = sender;
    }
    
    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }
    
    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }
    
    public Message copy() {
        return Message.builder()
                   .withCreatedDate(this.getCreatedDate())
                   .withCustomerId(this.getCustomerId())
                   .withIdentifier(this.getIdentifier())
                   .withMessageType(this.getMessageType())
                   .withResourceIdentifier(getResourceIdentifier())
                   .withOwner(this.getOwner())
                   .withSender(this.getSender())
                   .withText(this.getText())
                   .withResourceTitle(this.getResourceTitle())
                   .withModifiedDate(this.getModifiedDate())
                   .withTicketIdentifier(this.getTicketIdentifier())
                   .withStatus(this.getStatus())
                   .build();
    }
    
    public String getText() {
        return text;
    }
    
    public void setText(String text) {
        this.text = text;
    }
    
    public String getResourceTitle() {
        return resourceTitle;
    }
    
    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }
    
    private static MessageType calculateMessageType(TicketEntry ticketEntry) {
        if (ticketEntry instanceof DoiRequest) {
            return MessageType.DOI_REQUEST;
        }
        if (ticketEntry instanceof PublishingRequestCase) {
            return MessageType.PUBLISHING_REQUEST;
        }
        if (ticketEntry instanceof GeneralSupportRequest) {
            return MessageType.SUPPORT;
        }
        throw new UnsupportedOperationException("Unknown ticket type");
    }
    
    private static Builder buildMessage(UserInstance sender, Publication publication,
                                        String messageText, SortableIdentifier messageIdentifier,
                                        Clock clock) {
        
        var now = clock.instant();
        return Message.builder()
                   .withResourceIdentifier(publication.getIdentifier())
                   .withCustomerId(sender.getOrganizationUri())
                   .withText(messageText)
                   .withSender(sender.getUser())
                   .withOwner(new User(publication.getResourceOwner().getOwner()))
                   .withResourceTitle(extractTitle(publication))
                   .withCreatedDate(now)
                   .withModifiedDate(now)
                   .withIdentifier(messageIdentifier)
                   .withStatus(MessageStatus.UNREAD);
    }
    
    private static String extractTitle(Publication publication) {
        return Optional.of(publication)
                   .map(Publication::getEntityDescription)
                   .map(EntityDescription::getMainTitle)
                   .orElse(null);
    }
    
    public static final class Builder {
    
        private final Message message;
        
        private Builder() {
            message = new Message();
        }
        
        public Builder withIdentifier(SortableIdentifier identifier) {
            message.setIdentifier(identifier);
            return this;
        }
    
        public Builder withOwner(User owner) {
            message.setOwner(owner);
            return this;
        }
        
        public Builder withCustomerId(URI customerId) {
            message.setCustomerId(customerId);
            return this;
        }
    
        public Builder withSender(User sender) {
            message.setSender(sender);
            return this;
        }
        
        public Builder withResourceIdentifier(SortableIdentifier resourceIdentifier) {
            message.setResourceIdentifier(resourceIdentifier);
            return this;
        }
        
        public Builder withTicketIdentifier(SortableIdentifier ticketIdentifier) {
            message.setTicketIdentifier(ticketIdentifier);
            return this;
        }
        
        public Builder withText(String text) {
            message.setText(text);
            return this;
        }
        
        public Builder withCreatedDate(Instant createdDate) {
            message.setCreatedDate(createdDate);
            return this;
        }
        
        public Builder withModifiedDate(Instant modifiedDate) {
            message.setModifiedDate(modifiedDate);
            return this;
        }
    
        public Builder withResourceTitle(String resourceTitle) {
            message.setResourceTitle(resourceTitle);
            return this;
        }
    
        public Builder withMessageType(MessageType messageType) {
            message.setMessageType(messageType);
            return this;
        }
    
        public Message build() {
            return message;
        }
    
        public Builder withStatus(MessageStatus status) {
            message.setStatus(status);
            return this;
        }
    }
}
