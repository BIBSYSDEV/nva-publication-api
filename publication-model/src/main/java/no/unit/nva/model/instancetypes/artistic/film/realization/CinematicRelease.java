package no.unit.nva.model.instancetypes.artistic.film.realization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.time.Instant;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class CinematicRelease implements MovingPictureOutput {

    public static final String PLACE = "place";
    public static final String DATE = "date";

    @JsonProperty(PLACE)
    private final UnconfirmedPlace place;
    @JsonProperty(DATE)
    private final Instant date;
    @JsonProperty(SEQUENCE_FIELD)
    private final int sequence;

    public CinematicRelease(@JsonProperty(PLACE) UnconfirmedPlace place,
                            @JsonProperty(DATE) Instant date,
                            @JsonProperty(SEQUENCE_FIELD) int sequence) {
        this.place = place;
        this.date = date;
        this.sequence = sequence;
    }

    public UnconfirmedPlace getPlace() {
        return place;
    }

    public Instant getDate() {
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
        if (!(o instanceof CinematicRelease)) {
            return false;
        }
        CinematicRelease that = (CinematicRelease) o;
        return getSequence() == that.getSequence()
                && Objects.equals(getPlace(), that.getPlace())
                && Objects.equals(getDate(), that.getDate());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPlace(), getDate(), getSequence());
    }
}
