
package no.unit.nva.publication.ticket;

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
import no.unit.nva.publication.model.business.Message;
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
    private User senderIdentifier;
    @JsonProperty("owner")
    private User ownerIdentifier;
    @JsonProperty(TEXT_FIELD)
    private String text;
    @JsonProperty("date")
    private Instant date;
    @JsonProperty("messageType")
    private String messageType;
    @JsonProperty("recipient")
    private User recipient;
    
    public static MessageDto fromMessage(Message message) {
        MessageDto messageDto = new MessageDto();
        messageDto.setOwnerIdentifier(message.getOwner());
        messageDto.setSenderIdentifier(message.getSender());
        messageDto.setText(message.getText());
        messageDto.setDate(message.getCreatedDate());
        messageDto.setMessageId(constructMessageId(message.getIdentifier()));
        messageDto.setMessageIdentifier(message.getIdentifier());
        messageDto.setMessageType(message.getMessageType().toString());
        messageDto.setRecipient(message.getRecipient());
        return messageDto;
    }
    
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
            getSenderIdentifier(),
            getOwnerIdentifier(),
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
               && Objects.equals(getSenderIdentifier(), that.getSenderIdentifier())
               && Objects.equals(getOwnerIdentifier(), that.getOwnerIdentifier())
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
    public User getSenderIdentifier() {
        return senderIdentifier;
    }
    
    @JacocoGenerated
    public void setSenderIdentifier(User senderIdentifier) {
        this.senderIdentifier = senderIdentifier;
    }
    
    @JacocoGenerated
    public User getOwnerIdentifier() {
        return ownerIdentifier;
    }
    
    @JacocoGenerated
    public void setOwnerIdentifier(User ownerIdentifier) {
        this.ownerIdentifier = ownerIdentifier;
    }
}
