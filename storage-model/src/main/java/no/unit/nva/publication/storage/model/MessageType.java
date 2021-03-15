package no.unit.nva.publication.storage.model;

import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.JacocoGenerated;

public enum MessageType {
    SUPPORT("Support"),
    DOI_REQUEST("DoiRequest");

    protected static final String INVALID_MESSAGE_TYPE_ERROR = "Invalid Message type: ";

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    public static MessageType parse(String value) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.getValue().equalsIgnoreCase(value)) {
                return messageType;
            }
        }
        throw new IllegalArgumentException(INVALID_MESSAGE_TYPE_ERROR + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return getValue();
    }
}
