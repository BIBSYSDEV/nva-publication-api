package no.unit.nva.publication.model.business;

public enum MessageStatus {
    READ,
    UNREAD;
    
    public static final String INVALID_MESSAGE_STATUS = "Invalid MessageStatus: ";
    
    public static MessageStatus parse(String value) {
        for (MessageStatus messageStatus : MessageStatus.values()) {
            if (messageStatus.name().equalsIgnoreCase(value)) {
                return messageStatus;
            }
        }
        throw new IllegalArgumentException(INVALID_MESSAGE_STATUS + value);
    }
}
