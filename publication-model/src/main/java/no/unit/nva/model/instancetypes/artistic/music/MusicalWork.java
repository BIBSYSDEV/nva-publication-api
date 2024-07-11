package no.unit.nva.model.instancetypes.artistic.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class MusicalWork {

    public static final String TITLE = "title";
    public static final String COMPOSER = "composer";

    @JsonProperty(TITLE)
    private final String title;
    @JsonProperty(COMPOSER)
    private final String composer;

    @JsonCreator
    public MusicalWork(@JsonProperty(TITLE) String title,
                       @JsonProperty(COMPOSER) String composer) {
        this.title = title;
        this.composer = composer;
    }

    public String getTitle() {
        return title;
    }

    public String getComposer() {
        return composer;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getTitle(), getComposer());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MusicalWork)) {
            return false;
        }
        MusicalWork that = (MusicalWork) o;
        return Objects.equals(getTitle(), that.getTitle()) && Objects.equals(getComposer(),
                                                                             that.getComposer());
    }
}
