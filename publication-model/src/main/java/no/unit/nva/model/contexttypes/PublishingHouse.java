package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Marker pattern interface.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Publisher", value = Publisher.class),
    @JsonSubTypes.Type(name = "UnconfirmedPublisher", value = UnconfirmedPublisher.class),
    @JsonSubTypes.Type(name = "NullPublisher", value = NullPublisher.class)
})
public interface PublishingHouse {
    boolean isValid();
}
