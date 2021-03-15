package no.unit.nva.publication.storage.model;

import java.util.Arrays;
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
        return Arrays.stream(MessageType.values())
                   .filter(messageType -> messageType.getValue().equals(value))
                   .findFirst()
                   .orElseThrow(() -> handleParsingError(value));
    }

    public String getValue() {
        return value;
    }

    @Override
    @JacocoGenerated
    public String toString() {
        return getValue();
    }

    private static IllegalArgumentException handleParsingError(String value) {
        return new IllegalArgumentException(INVALID_MESSAGE_TYPE_ERROR + value);
    }
}
