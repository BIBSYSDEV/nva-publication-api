package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.Entity.nextVersion;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.MessageDao;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
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
    private TicketStatus status;
    @JsonProperty("sender")
    private String sender;
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
    @JsonAlias("rowVersion")
    @JsonProperty("version")
    private UUID version;
    
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
                   .withVersion(UUID.randomUUID())
                   .withCustomerId(ticket.getCustomerId())
                   .withOwner(ticket.getOwner())
                   .withMessageType(calculateMessageType(ticket))
                   .withIdentifier(SortableIdentifier.next())
                   .withText(message)
                   .withSender(sender.getUserIdentifier())
                   .withResourceTitle("NOT_USED")
                   .withStatus(TicketStatus.UNREAD)
                   .withResourceIdentifier(ticket.getResourceIdentifier())
                   .withTicketIdentifier(ticket.getIdentifier())
                   .build();
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
    
    public SortableIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }
    
    public void setTicketIdentifier(SortableIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
    }
    
    @JsonProperty("recipient")
    public String getRecipient() {
        return owner.equals(sender) ? SUPPORT_SERVICE_RECIPIENT : owner;
    }
    
    public void setRecipient(String recipient) {
        // DO NOTHING
    }
    
    public Message markAsRead(Clock clock) {
        var copy = this.copy();
        copy.setStatus(TicketStatus.READ);
        copy.setModifiedDate(clock.instant());
        copy.setVersion(UUID.randomUUID());
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
    
    @JacocoGenerated
    @Override
    public UUID getVersion() {
        return this.version;
    }
    
    @Override
    @JacocoGenerated
    public void setVersion(UUID version) {
        this.version = version;
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
    public String getOwner() {
        return owner;
    }
    
    @Override
    public URI getCustomerId() {
        return customerId;
    }
    
    //TODO: cover this method when Message is not a ticket any more.
    @JacocoGenerated
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
    
    public void setOwner(String owner) {
        this.owner = owner;
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
    public void validateCreationRequirements(Publication publication) {
        throw new UnsupportedOperationException();
    }
    
    //TODO: remove method or cover when Message is not a Ticket anymore.
    @JacocoGenerated
    @Override
    public void validateCompletionRequirements(Publication publication) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public Message copy() {
        return Message.builder()
                   .withCreatedDate(this.getCreatedDate())
                   .withCustomerId(this.getCustomerId())
                   .withIdentifier(this.getIdentifier())
                   .withMessageType(this.getMessageType())
                   .withResourceIdentifier(getResourceIdentifier())
                   .withStatus(this.getStatus())
                   .withOwner(this.getOwner())
                   .withSender(this.getSender())
                   .withText(this.getText())
                   .withResourceTitle(this.getResourceTitle())
                   .withModifiedDate(this.getModifiedDate())
                   .withVersion(this.getVersion())
                   .withTicketIdentifier(this.getTicketIdentifier())
                   .build();
    }
    
    @Override
    public TicketStatus getStatus() {
        return status;
    }
    
    @Override
    public void setStatus(TicketStatus status) {
        this.status = status;
    }
    
    @JacocoGenerated
    @Override
    public List<Message> fetchMessages(TicketService ticketService) {
        throw new UnsupportedOperationException();
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
    
    public String getResourceTitle() {
        return resourceTitle;
    }
    
    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getOwner(), getCustomerId(), getStatus(), getSender(),
            getResourceIdentifier(),
            getTicketIdentifier(), getText(), getCreatedDate(), getModifiedDate(), getResourceTitle(),
            getMessageType());
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
                   .withStatus(TicketStatus.UNREAD)
                   .withResourceIdentifier(publication.getIdentifier())
                   .withCustomerId(sender.getOrganizationUri())
                   .withText(messageText)
                   .withSender(sender.getUserIdentifier())
                   .withOwner(publication.getResourceOwner().getOwner())
                   .withResourceTitle(extractTitle(publication))
                   .withCreatedDate(now)
                   .withModifiedDate(now)
                   .withIdentifier(messageIdentifier)
                   .withVersion(nextVersion());
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
        
        public Builder withOwner(String owner) {
            message.setOwner(owner);
            return this;
        }
        
        public Builder withCustomerId(URI customerId) {
            message.setCustomerId(customerId);
            return this;
        }
        
        public Builder withStatus(TicketStatus status) {
            message.setStatus(status);
            return this;
        }
        
        public Builder withSender(String sender) {
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
        
        public Builder withVersion(UUID version) {
            message.setVersion(version);
            return this;
        }
        
        public Message build() {
            return message;
        }
    }
}
