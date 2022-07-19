package no.unit.nva.publication.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.MESSAGE_PATH;
import static no.unit.nva.publication.PublicationServiceConfig.PATH_SEPARATOR;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Objects;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.storage.model.Message;
import nva.commons.core.JacocoGenerated;

public class MessageDto implements JsonSerializable {
    
    public static final String TEXT_FIELD = "text";
    @JsonProperty("id")
    private URI messageId;
    @JsonProperty("identifier")
    private SortableIdentifier messageIdentifier;
    @JsonProperty("sender")
    private String senderIdentifier;
    @JsonProperty("owner")
    private String ownerIdentifier;
    @JsonProperty(TEXT_FIELD)
    private String text;
    @JsonProperty("date")
    private Instant date;
    @JsonProperty("messageType")
    private String messageType;
    
    public static MessageDto fromMessage(Message message) {
        MessageDto messageDto = new MessageDto();
        messageDto.setOwnerIdentifier(message.getOwner());
        messageDto.setSenderIdentifier(message.getSender());
        messageDto.setText(message.getText());
        messageDto.setDate(message.getCreatedTime());
        messageDto.setMessageId(constructMessageId(message.getIdentifier()));
        messageDto.setMessageIdentifier(message.getIdentifier());
        messageDto.setMessageType(message.getMessageType().toString());
        return messageDto;
    }
    
    public static URI constructMessageId(SortableIdentifier messageIdentifier) {
        if (nonNull(messageIdentifier)) {
            String scheme = PublicationServiceConfig.API_SCHEME;
            String host = PublicationServiceConfig.API_HOST;
            String messagePath = MESSAGE_PATH + PATH_SEPARATOR + messageIdentifier.toString();
            return attempt(() -> newUri(scheme, host, messagePath)).orElseThrow();
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
    public String getSenderIdentifier() {
        return senderIdentifier;
    }
    
    @JacocoGenerated
    public void setSenderIdentifier(String senderIdentifier) {
        this.senderIdentifier = senderIdentifier;
    }
    
    @JacocoGenerated
    public String getOwnerIdentifier() {
        return ownerIdentifier;
    }
    
    @JacocoGenerated
    public void setOwnerIdentifier(String ownerIdentifier) {
        this.ownerIdentifier = ownerIdentifier;
    }
    
    private static URI newUri(String scheme, String host, String messagesPath) throws URISyntaxException {
        return new URI(scheme, host, messagesPath, PublicationServiceConfig.URI_EMPTY_FRAGMENT);
    }
}
