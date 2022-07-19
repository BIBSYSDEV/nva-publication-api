package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.stream.Collectors;
import nva.commons.core.JacocoGenerated;

public enum MessageType {
    SUPPORT("Support"),
    DOI_REQUEST("DoiRequest"),
    PUBLISHING_REQUEST("PublishingRequest");
    
    public static final String DELIMITER = ", ";
    static final String INVALID_MESSAGE_TYPE_ERROR =
        String.format("Invalid Message type. Allowed values: %s", allowedValuesString());
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
        throw new IllegalArgumentException(INVALID_MESSAGE_TYPE_ERROR);
    }
    
    public static String allowedValuesString() {
        return Arrays.stream(MessageType.values())
            .map(MessageType::toString)
            .collect(Collectors.joining(DELIMITER));
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
