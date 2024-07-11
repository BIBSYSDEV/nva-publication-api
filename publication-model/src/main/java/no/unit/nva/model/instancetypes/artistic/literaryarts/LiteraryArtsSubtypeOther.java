package no.unit.nva.model.instancetypes.artistic.literaryarts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class LiteraryArtsSubtypeOther extends LiteraryArtsSubtype {
    @JsonProperty(DESCRIPTION_FIELD)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;

    @JsonCreator
    public LiteraryArtsSubtypeOther(@JsonProperty(TYPE_FIELD) LiteraryArtsSubtypeEnum type,
                                    @JsonProperty(DESCRIPTION_FIELD) String description) {
        super(type);
        this.description = description;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LiteraryArtsSubtypeOther)) {
            return false;
        }
        LiteraryArtsSubtypeOther that = (LiteraryArtsSubtypeOther) o;
        return Objects.equals(description, that.description);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(description);
    }
}
