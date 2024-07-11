package no.unit.nva.model.instancetypes.artistic.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class MusicMediaSubtypeOther extends MusicMediaSubtype {

    private final String description;

    @JsonCreator
    public MusicMediaSubtypeOther(@JsonProperty(TYPE_FIELD) MusicMediaType type,
                                  @JsonProperty() String description) {
        super(type);

        this.description = description;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDescription());
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
        if (!super.equals(o)) {
            return false;
        }
        MusicMediaSubtypeOther that = (MusicMediaSubtypeOther) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    public String getDescription() {
        return description;
    }
}
