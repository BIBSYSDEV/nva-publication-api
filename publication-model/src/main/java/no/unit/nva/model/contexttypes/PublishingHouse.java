package no.unit.nva.model.contexttypes;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Marker pattern interface.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Publisher", value = Publisher.class),
    @JsonSubTypes.Type(name = "UnconfirmedPublisher", value = UnconfirmedPublisher.class),
    @JsonSubTypes.Type(name = "NullPublisher", value = NullPublisher.class)
})
@Schema(oneOf = {Publisher.class, UnconfirmedPublisher.class, NullPublisher.class})
public interface PublishingHouse {
    boolean isValid();
}
