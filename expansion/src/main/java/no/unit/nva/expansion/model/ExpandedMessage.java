package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(ExpandedMessage.TYPE)
public class ExpandedMessage implements JsonSerializable {

    public static final String TYPE = "Message";
    public static final String TEXT_FIELD = "text";
    public static final String IDENTIFIER = "identifier";
    public static final String SENDER = "sender";
    public static final String OWNER = "owner";
    public static final String CREATED_DATE = "createdDate";
    public static final String CUSTOMER_ID = "customerId";
    public static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    public static final String TICKET_IDENTIFIER = "ticketIdentifier";
    public static final String CREATED_TIME = "createdTime";
    public static final String MODIFIED_TIME = "modifiedTime";
    public static final String MODIFIED_DATE = "modifiedDate";
    public static final String RESOURCE_TITLE = "resourceTitle";
    @JsonProperty(IDENTIFIER)
    private SortableIdentifier identifier;
    @JsonProperty(OWNER)
    private User owner;
    @JsonProperty(CUSTOMER_ID)
    private URI customerId;
    @JsonProperty(SENDER)
    private ExpandedPerson sender;
    @JsonProperty(RESOURCE_IDENTIFIER)
    private SortableIdentifier resourceIdentifier;
    @JsonProperty(TICKET_IDENTIFIER)
    private SortableIdentifier ticketIdentifier;
    @JsonProperty(TEXT_FIELD)
    private String text;
    //TODO: remove alias after migration
    @JsonAlias(CREATED_TIME)
    @JsonProperty(CREATED_DATE)
    private Instant createdDate;
    //TODO: remove alias after migration
    @JsonAlias(MODIFIED_TIME)
    @JsonProperty(MODIFIED_DATE)
    private Instant modifiedDate;
    @JsonProperty(RESOURCE_TITLE)
    private String resourceTitle;

    @JacocoGenerated
    public ExpandedMessage() {
    }

    public static ExpandedMessage createEntry(Message message,
                                              ResourceExpansionService expansionService) {
        return builder()
                .withCreatedDate(message.getCreatedDate())
                .withCustomerId(message.getCustomerId())
                .withIdentifier(message.getIdentifier())
                .withResourceIdentifier(message.getResourceIdentifier())
                .withOwner(message.getOwner())
                .withSender(expansionService.expandPerson(message.getSender()))
                .withText(message.getText())
                .withResourceTitle(message.getResourceTitle())
                .withModifiedDate(message.getModifiedDate())
                .withTicketIdentifier(message.getTicketIdentifier())
                .build();

    }

    public static Builder builder() {
        return new Builder();
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    public ExpandedPerson getSender() {
        return sender;
    }

    public void setSender(ExpandedPerson sender) {
        this.sender = sender;
    }

    public SortableIdentifier getResourceIdentifier() {
        return resourceIdentifier;
    }

    public void setResourceIdentifier(SortableIdentifier resourceIdentifier) {
        this.resourceIdentifier = resourceIdentifier;
    }

    public SortableIdentifier getTicketIdentifier() {
        return ticketIdentifier;
    }

    public void setTicketIdentifier(SortableIdentifier ticketIdentifier) {
        this.ticketIdentifier = ticketIdentifier;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getResourceTitle() {
        return resourceTitle;
    }

    public void setResourceTitle(String resourceTitle) {
        this.resourceTitle = resourceTitle;
    }

    public Message toMessage() {
        return Message.builder()
                .withCreatedDate(this.getCreatedDate())
                .withCustomerId(this.getCustomerId())
                .withIdentifier(this.getIdentifier())
                .withResourceIdentifier(getResourceIdentifier())
                .withOwner(this.getOwner())
                .withSender(this.getSender().getUsername())
                .withText(this.getText())
                .withResourceTitle(this.getResourceTitle())
                .withModifiedDate(this.getModifiedDate())
                .withTicketIdentifier(this.getTicketIdentifier())
                .build();
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        ExpandedMessage that = (ExpandedMessage) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
                && Objects.equals(getOwner(), that.getOwner())
                && Objects.equals(getCustomerId(), that.getCustomerId())
                && Objects.equals(getSender(), that.getSender())
                && Objects.equals(getResourceIdentifier(), that.getResourceIdentifier())
                && Objects.equals(getTicketIdentifier(), that.getTicketIdentifier())
                && Objects.equals(getText(), that.getText())
                && Objects.equals(getCreatedDate(), that.getCreatedDate())
                && Objects.equals(getModifiedDate(), that.getModifiedDate())
                && Objects.equals(getResourceTitle(), that.getResourceTitle());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(),
                getOwner(),
                getCustomerId(),
                getSender(),
                getResourceIdentifier(),
                getTicketIdentifier(),
                getText(),
                getCreatedDate(),
                getModifiedDate(),
                getResourceTitle());
    }

    public static final class Builder {

        private final ExpandedMessage message;

        private Builder() {
            message = new ExpandedMessage();
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

        public Builder withSender(ExpandedPerson sender) {
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

        public ExpandedMessage build() {
            return message;
        }
    }
}
