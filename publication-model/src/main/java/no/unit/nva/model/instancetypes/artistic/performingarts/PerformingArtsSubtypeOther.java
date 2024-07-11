package no.unit.nva.model.instancetypes.artistic.performingarts;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class PerformingArtsSubtypeOther extends PerformingArtsSubtype {
    private static final String DESCRIPTION = "description";

    @JsonProperty(DESCRIPTION)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;

    protected PerformingArtsSubtypeOther(@JsonProperty(TYPE) PerformingArtsSubtypeEnum type,
                                        @JsonProperty(DESCRIPTION) String description) {
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
        if (!(o instanceof PerformingArtsSubtypeOther)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PerformingArtsSubtypeOther that = (PerformingArtsSubtypeOther) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDescription());
    }
}
