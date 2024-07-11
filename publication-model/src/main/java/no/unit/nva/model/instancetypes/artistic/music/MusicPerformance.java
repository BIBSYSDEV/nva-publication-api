package no.unit.nva.model.instancetypes.artistic.music;

import static no.unit.nva.model.util.SerializationUtils.nullListAsEmpty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Objects;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.NullPages;
import no.unit.nva.model.time.duration.Duration;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MusicPerformance implements PublicationInstance<NullPages> {

    public static final String MANIFESTATIONS_FIELD = "manifestations";
    public static final String DURATION_FIELD = "duration";
    @JsonProperty(MANIFESTATIONS_FIELD)
    private final List<MusicPerformanceManifestation> manifestations;
    @JsonProperty(DURATION_FIELD)
    private final Duration duration;

    @JsonCreator
    public MusicPerformance(@JsonProperty(MANIFESTATIONS_FIELD) List<MusicPerformanceManifestation> manifestations,
                            @JsonProperty(DURATION_FIELD) Duration duration) {
        this.manifestations = nullListAsEmpty(manifestations);
        this.duration = duration;
    }

    public List<MusicPerformanceManifestation> getManifestations() {
        return manifestations;
    }

    @Override
    public NullPages getPages() {
        return NullPages.NULL_PAGES;
    }

    public Duration getDuration() {
        return duration;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MusicPerformance)) {
            return false;
        }
        MusicPerformance that = (MusicPerformance) o;
        return Objects.equals(getManifestations(), that.getManifestations())
            && Objects.equals(getDuration(), that.getDuration());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getManifestations(), getDuration());
    }
}
