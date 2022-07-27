package no.unit.nva.publication.messages.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = MarkMessageAsReadRequest.TYPE, value = MarkMessageAsReadRequest.class)
})
public interface UpdateMessageRequest {

}
