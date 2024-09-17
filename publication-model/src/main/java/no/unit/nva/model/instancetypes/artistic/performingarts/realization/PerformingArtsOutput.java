package no.unit.nva.model.instancetypes.artistic.performingarts.realization;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "PerformingArtsVenue", value = PerformingArtsVenue.class)
})
public interface PerformingArtsOutput {

}
