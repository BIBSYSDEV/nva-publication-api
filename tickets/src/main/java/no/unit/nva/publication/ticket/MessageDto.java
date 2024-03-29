package no.unit.nva.publication.ticket;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_HOST_URI;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.MessageStatus;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(MessageDto.TYPE)
public class MessageDto implements JsonSerializable {
    
    public static final String TEXT_FIELD = "text";
    public static final String TYPE = "Message";
    public static final String NO_TEXT = null;
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
    @JsonProperty("createdDate")
    @JsonAlias("date")
    private Instant createdDate;
    @JsonProperty("messageType")
    private String messageType;
    @JsonProperty("status")
    private MessageStatus status;
    
    public static MessageDto fromMessage(Message message) {
        MessageDto messageDto = new MessageDto();
        messageDto.setOwnerIdentifier(message.getOwner());
        messageDto.setSenderIdentifier(message.getSender());
        messageDto.setText(getMessageText(message));
        messageDto.setCreatedDate(message.getCreatedDate());
        messageDto.setMessageId(constructMessageId(message));
        messageDto.setMessageIdentifier(message.getIdentifier());
        return messageDto;
    }

    private static String getMessageText(Message message) {
        return MessageStatus.ACTIVE.equals(message.getStatus()) ? message.getText() : NO_TEXT;
    }

    public static URI constructMessageId(Message message) {
        if (nonNull(message.getIdentifier())) {
            return UriWrapper.fromUri(PUBLICATION_HOST_URI)
                       .addChild(message.getResourceIdentifier().toString())
                       .addChild(PublicationServiceConfig.TICKET_PATH)
                       .addChild(message.getTicketIdentifier().toString())
                       .addChild(PublicationServiceConfig.MESSAGE_PATH)
                       .addChild(message.getIdentifier().toString())
                       .getUri();
        }
        return null;
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
                            getCreatedDate(),
                            getMessageType(),
                            getStatus());
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
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getStatus(), that.getStatus());
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
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    @JacocoGenerated
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
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

    @JacocoGenerated
    public MessageStatus getStatus() {
        return nonNull(status) ? status : MessageStatus.ACTIVE;
    }

    @JacocoGenerated
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
}
