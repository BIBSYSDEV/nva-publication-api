package no.unit.nva.model.instancetypes.artistic.music;

import static no.unit.nva.model.util.SerializationUtils.nullListAsEmpty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.List;
import java.util.Objects;
import no.unit.nva.model.contexttypes.place.UnconfirmedPlace;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class OtherPerformance implements MusicPerformanceManifestation {

    public static final String PERFORMANCE_TYPE = "performanceType";
    public static final String PLACE = "place";
    public static final String EXTENT = "extent";
    public static final String MUSICAL_WORKS = "musicalWorks";
    @JsonProperty(PERFORMANCE_TYPE)
    private final String performanceType;
    @JsonProperty(PLACE)
    private final UnconfirmedPlace place;
    @JsonProperty(EXTENT)
    private final String extent;

    // TODO: Migrate to outputs
    @JsonProperty(MUSICAL_WORKS)
    private final List<MusicalWork> musicalWorks;

    @JsonCreator
    public OtherPerformance(@JsonProperty(PERFORMANCE_TYPE) String performanceType,
                            @JsonProperty(PLACE) UnconfirmedPlace place,
                            @JsonProperty(EXTENT) String extent,
                            @JsonProperty(MUSICAL_WORKS) List<MusicalWork> musicalWorks) {
        this.performanceType = performanceType;
        this.place = place;
        this.extent = extent;
        this.musicalWorks = nullListAsEmpty(musicalWorks);
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof OtherPerformance)) {
            return false;
        }
        OtherPerformance that = (OtherPerformance) o;
        return Objects.equals(getPerformanceType(), that.getPerformanceType())
               && Objects.equals(getPlace(), that.getPlace())
               && Objects.equals(getExtent(), that.getExtent())
               && Objects.equals(getMusicalWorks(), that.getMusicalWorks());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPerformanceType(), getPlace(), getExtent(), getMusicalWorks());
    }

    public String getPerformanceType() {
        return performanceType;
    }

    public UnconfirmedPlace getPlace() {
        return place;
    }

    public String getExtent() {
        return extent;
    }

    public List<MusicalWork> getMusicalWorks() {
        return musicalWorks;
    }
}
