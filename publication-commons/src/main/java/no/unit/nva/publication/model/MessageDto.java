package no.unit.nva.publication.model;

import java.util.Objects;
import no.unit.nva.publication.storage.model.Message;
import nva.commons.core.JacocoGenerated;

public class MessageDto {

    private String senderIdentifier;
    private String ownerIdentifier;
    private String text;

    public static MessageDto fromMessage(Message message) {
        MessageDto messageDto = new MessageDto();
        messageDto.setOwnerIdentifier(message.getOwner());
        messageDto.setSenderIdentifier(message.getSender());
        messageDto.setText(message.getText());
        return messageDto;
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

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getSenderIdentifier(), getOwnerIdentifier(), getText());
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
        return Objects.equals(getSenderIdentifier(), that.getSenderIdentifier()) && Objects.equals(
            getOwnerIdentifier(), that.getOwnerIdentifier()) && Objects.equals(getText(), that.getText());
    }
}
