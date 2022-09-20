package no.unit.nva.publication.messages.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.MESSAGE_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_HOST_URI;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class MessageDto implements JsonSerializable {
    
    public static final String TEXT_FIELD = "text";
    @JsonProperty("id")
    private URI messageId;
    @JsonProperty("identifier")
    private SortableIdentifier messageIdentifier;
    @JsonProperty("sender")
    private User sender;
    @JsonProperty("owner")
    private User owner;
    @JsonProperty(TEXT_FIELD)
    private String text;
    @JsonProperty("date")
    private Instant date;
    @JsonProperty("messageType")
    private String messageType;
    @JsonProperty("recipient")
    private User recipient;
    
    public static URI constructMessageId(SortableIdentifier messageIdentifier) {
        if (nonNull(messageIdentifier)) {
            return UriWrapper.fromUri(PUBLICATION_HOST_URI)
                       .addChild(MESSAGE_PATH)
                       .addChild(messageIdentifier.toString())
                       .getUri();
        }
        return null;
    }
    
    @JacocoGenerated
    public User getRecipient() {
        return recipient;
    }
    
    @JacocoGenerated
    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }
    
    @JacocoGenerated
    public String getMessageType() {
        return messageType;
    }
    
    @JacocoGenerated
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getMessageId(),
            getMessageIdentifier(),
            getSender(),
            getOwner(),
            getText(),
            getDate(),
            getMessageType());
    }
    
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MessageDto)) {
            return false;
        }
        MessageDto that = (MessageDto) o;
        return Objects.equals(getMessageType(), that.getMessageType())
               && Objects.equals(getMessageId(), that.getMessageId())
               && Objects.equals(getMessageIdentifier(), that.getMessageIdentifier())
               && Objects.equals(getSender(), that.getSender())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getText(), that.getText())
               && Objects.equals(getDate(), that.getDate());
    }
    
    @Override
    @JacocoGenerated
    public String toString() {
        return toJsonString();
    }
    
    @JacocoGenerated
    public URI getMessageId() {
        return messageId;
    }
    
    @JacocoGenerated
    public void setMessageId(URI messageId) {
        this.messageId = messageId;
    }
    
    @JacocoGenerated
    public SortableIdentifier getMessageIdentifier() {
        return messageIdentifier;
    }
    
    @JacocoGenerated
    public void setMessageIdentifier(SortableIdentifier messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
    }
    
    @JacocoGenerated
    public Instant getDate() {
        return date;
    }
    
    @JacocoGenerated
    public void setDate(Instant date) {
        this.date = date;
    }
    
    @JacocoGenerated
    public String getText() {
        return text;
    }
    
    @JacocoGenerated
    public void setText(String text) {
        this.text = text;
    }
    
    @JacocoGenerated
    public User getSender() {
        return sender;
    }
    
    @JacocoGenerated
    public void setSender(User sender) {
        this.sender = sender;
    }
    
    @JacocoGenerated
    public User getOwner() {
        return owner;
    }
    
    @JacocoGenerated
    public void setOwner(User owner) {
        this.owner = owner;
    }
}
