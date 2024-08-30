package no.unit.nva.model.contexttypes.place;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "UnconfirmedPlace", value = UnconfirmedPlace.class)
})
@Schema(oneOf = {UnconfirmedPlace.class})
public interface Place {
    // This class is used to allow extension
}
