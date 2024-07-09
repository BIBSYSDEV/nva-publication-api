package no.unit.nva.model.instancetypes.artistic.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MusicTrack extends MusicalWork {

    public static final String EXTENT = "extent";
    @JsonProperty(EXTENT)
    private final String extent;

    @JsonCreator
    public MusicTrack(@JsonProperty(TITLE) String title,
                      @JsonProperty(COMPOSER) String composer,
                      @JsonProperty(EXTENT) String extent) {
        super(title, composer);
        this.extent = extent;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(super.hashCode(), getExtent());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MusicTrack)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MusicTrack that = (MusicTrack) o;
        return Objects.equals(getExtent(), that.getExtent());
    }

    public String getExtent() {
        return extent;
    }
}
