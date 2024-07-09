package no.unit.nva.model.instancetypes.artistic.visualarts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class VisualArtsSubtypeOther extends VisualArtsSubtype {
    @JsonProperty(DESCRIPTION_FIELD)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;

    public VisualArtsSubtypeOther(@JsonProperty(TYPE_FIELD) VisualArtsSubtypeEnum type,
                                  @JsonProperty(DESCRIPTION_FIELD) String description) {
        super(type);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VisualArtsSubtypeOther)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        VisualArtsSubtypeOther that = (VisualArtsSubtypeOther) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDescription());
    }
}
