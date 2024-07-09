package no.unit.nva.model.instancetypes.artistic.music;

import static no.unit.nva.model.util.SerializationUtils.nullListAsEmpty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;

public class Concert implements MusicPerformanceManifestation {

    public static final String PLACE_FIELD = "place";
    public static final String TIME_FIELD = "time";
    public static final String EXTENT_FIELD = "extent";
    public static final String CONCERT_PROGRAMME_FIELD = "concertProgramme";
    public static final String CONCERT_SERIES_FIELD = "concertSeries";

    @JsonProperty(PLACE_FIELD)
    private final UnconfirmedPlace place;
    @JsonProperty(TIME_FIELD)
    private final Time time;
    @JsonProperty(EXTENT_FIELD)
    private final String extent;
    @JsonProperty(CONCERT_PROGRAMME_FIELD)
    private final List<MusicalWorkPerformance> concertProgramme;
    @JsonProperty(CONCERT_SERIES_FIELD)
    private final String concertSeries;

    @JsonCreator
    public Concert(@JsonProperty(PLACE_FIELD) UnconfirmedPlace place,
                   @JsonProperty(TIME_FIELD) Time time,
                   @JsonProperty(EXTENT_FIELD) String extent,
                   @JsonProperty(CONCERT_PROGRAMME_FIELD) List<MusicalWorkPerformance> concertProgramme,
                   @JsonProperty(CONCERT_SERIES_FIELD) String concertSeries) {
        this.place = place;
        this.time = time;
        this.extent = extent;
        this.concertProgramme = nullListAsEmpty(concertProgramme);
        this.concertSeries = concertSeries;
    }

    public UnconfirmedPlace getPlace() {
        return place;
    }

    public Time getTime() {
        return time;
    }

    public String getExtent() {
        return extent;
    }

    public List<MusicalWorkPerformance> getConcertProgramme() {
        return concertProgramme;
    }

    public String getConcertSeries() {
        return concertSeries;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Concert concert = (Concert) o;
        return Objects.equals(getPlace(), concert.getPlace())
               && Objects.equals(getTime(), concert.getTime())
               && Objects.equals(getExtent(), concert.getExtent())
               && Objects.equals(getConcertProgramme(), concert.getConcertProgramme())
               && Objects.equals(getConcertSeries(), concert.getConcertSeries());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getPlace(), getTime(), getExtent(), getConcertProgramme(), getConcertSeries());
    }
}
