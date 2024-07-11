package no.unit.nva.model.instancetypes.artistic.performingarts.realization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.contexttypes.place.Place;
import no.unit.nva.model.instancetypes.artistic.design.realization.Venue;
import no.unit.nva.model.time.Time;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PerformingArtsVenue extends Venue implements PerformingArtsOutput {
    public PerformingArtsVenue(@JsonProperty(PLACE) Place place,
                               @JsonProperty(DATE) Time date,
                               @JsonProperty(SEQUENCE_FIELD) int sequence) {
        super(place, date, sequence);
    }
}
