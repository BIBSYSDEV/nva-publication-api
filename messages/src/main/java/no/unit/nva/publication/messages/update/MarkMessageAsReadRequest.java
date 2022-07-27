package no.unit.nva.publication.messages.update;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class MarkMessageAsReadRequest implements UpdateMessageRequest {
    
    public static final String TYPE = "MarkMessageAsRead";
}
