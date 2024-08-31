package no.unit.nva.model.instancetypes.artistic.performingarts.realization;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "PerformingArtsVenue", value = PerformingArtsVenue.class)
})
@Schema(oneOf = {PerformingArtsVenue.class})
public interface PerformingArtsOutput {

}
