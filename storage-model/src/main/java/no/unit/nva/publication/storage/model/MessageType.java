package no.unit.nva.publication.storage.model;

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

    public String getValue() {
        return value;
    }
}
