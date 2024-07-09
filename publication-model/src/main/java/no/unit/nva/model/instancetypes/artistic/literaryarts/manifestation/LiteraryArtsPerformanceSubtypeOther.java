package no.unit.nva.model.instancetypes.artistic.literaryarts.manifestation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class LiteraryArtsPerformanceSubtypeOther extends LiteraryArtsPerformanceSubtype {
    @JsonProperty(DESCRIPTION_FIELD)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;
    
    public LiteraryArtsPerformanceSubtypeOther(@JsonProperty(TYPE_FIELD) LiteraryArtsPerformanceSubtypeEnum type,
                                               @JsonProperty(DESCRIPTION_FIELD)String description) {
        super(type);
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArtsPerformanceSubtypeOther)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        LiteraryArtsPerformanceSubtypeOther that = (LiteraryArtsPerformanceSubtypeOther) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDescription());
    }
}
