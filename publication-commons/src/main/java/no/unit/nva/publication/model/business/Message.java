package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.MessageDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount"})
public class Message implements Entity, JsonSerializable {

    public static final String TYPE = "Message";

    @JsonProperty("identifier")
    private SortableIdentifier identifier;
    @JsonProperty("owner")
    private User owner;
    @JsonProperty("customerId")
    private URI customerId;
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
                   .withIdentifier(SortableIdentifier.next())
                   .withText(message)
                   .withSender(sender.getUser())
                   .withResourceTitle("NOT_USED")
                   .withResourceIdentifier(ticket.getResourceIdentifier())
                   .withTicketIdentifier(ticket.getIdentifier())
                   .build();
    }

    public SortableIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }

    public void setTicketIdentifier(SortableIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getOwner(), getCustomerId(), getSender(), getResourceIdentifier(),
                            getTicketIdentifier(), getText(), getCreatedDate(), getModifiedDate(), getResourceTitle());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Message message)) {
            return false;
        }
        return Objects.equals(getIdentifier(), message.getIdentifier())
               && Objects.equals(getOwner(), message.getOwner())
               && Objects.equals(getCustomerId(), message.getCustomerId())
               && Objects.equals(getSender(), message.getSender())
               && Objects.equals(getResourceIdentifier(), message.getResourceIdentifier())
               && Objects.equals(getTicketIdentifier(), message.getTicketIdentifier())
               && Objects.equals(getText(), message.getText())
               && Objects.equals(getCreatedDate(), message.getCreatedDate())
               && Objects.equals(getModifiedDate(), message.getModifiedDate())
               && Objects.equals(getResourceTitle(), message.getResourceTitle());
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
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
    public Publication toPublication(ResourceService resourceService) {
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
        return "NO_STATUS";
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
                   .withResourceIdentifier(getResourceIdentifier())
                   .withOwner(this.getOwner())
                   .withSender(this.getSender())
                   .withText(this.getText())
                   .withResourceTitle(this.getResourceTitle())
                   .withModifiedDate(this.getModifiedDate())
                   .withTicketIdentifier(this.getTicketIdentifier())
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

        public Message build() {
            return message;
        }
    }
}
