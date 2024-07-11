package no.unit.nva.model.instancetypes.artistic.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class MusicalWorkPerformance extends MusicalWork {

    public static final String PREMIERE = "premiere";

    @JsonProperty(PREMIERE)
    private final boolean premiere;

    @JsonCreator
    public MusicalWorkPerformance(@JsonProperty(TITLE) String title,
                                  @JsonProperty(COMPOSER) String composer,
                                  @JsonProperty(PREMIERE) boolean premiere) {
        super(title, composer);
        this.premiere = premiere;
    }

    public boolean isPremiere() {
        return premiere;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(super.hashCode(), isPremiere());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (!(o instanceof MusicalWorkPerformance)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MusicalWorkPerformance that = (MusicalWorkPerformance) o;
        return isPremiere() == that.isPremiere();
    }
}
