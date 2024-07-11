package no.unit.nva.model.instancetypes.artistic.design.realization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import no.unit.nva.model.contexttypes.place.Place;
import no.unit.nva.model.instancetypes.realization.WithSequence;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Venue implements WithSequence {
    public static final String PLACE = "place";
    public static final String DATE = "date";

    @JsonProperty(PLACE)
    private final Place place;
    @JsonProperty(SEQUENCE_FIELD)
    private final int sequence;
    @JsonProperty(DATE)
    private final Time date;

    public Venue(@JsonProperty(PLACE) Place place,
                 @JsonProperty(DATE) Time date,
                 @JsonProperty(SEQUENCE_FIELD) int sequence) {
        this.place = place;
        this.date = date;
        this.sequence = sequence;
    }

    public Place getPlace() {
        return place;
    }


    public Time getDate() {
        return date;
    }

    @Override
    public int getSequence() {
        return sequence;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Venue)) {
            return false;
        }
        Venue venue = (Venue) o;
        return getSequence() == venue.getSequence()
                && Objects.equals(getPlace(), venue.getPlace())
                && Objects.equals(getDate(), venue.getDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPlace(), getSequence(), getDate());
    }
}
