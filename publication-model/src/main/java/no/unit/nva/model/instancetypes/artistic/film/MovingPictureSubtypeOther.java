package no.unit.nva.model.instancetypes.artistic.film;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class MovingPictureSubtypeOther extends MovingPictureSubtype {

    private static final String DESCRIPTION = "description";

    @JsonProperty(DESCRIPTION)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;

    protected MovingPictureSubtypeOther(@JsonProperty(TYPE) MovingPictureSubtypeEnum type,
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

        if (!(o instanceof MovingPictureSubtypeOther)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        MovingPictureSubtypeOther that = (MovingPictureSubtypeOther) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDescription());
    }
}
