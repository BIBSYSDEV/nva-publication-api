package no.unit.nva.model.instancetypes.artistic.visualarts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class VisualArtsSubtype {

    public static final String TYPE_FIELD = "type";
    public static final String DESCRIPTION_FIELD = "description";
    @JsonProperty(TYPE_FIELD)
    private final VisualArtsSubtypeEnum type;

    protected VisualArtsSubtype(VisualArtsSubtypeEnum type) {
        this.type = type;
    }

    public static VisualArtsSubtype createOther(String description) {
        return new VisualArtsSubtypeOther(VisualArtsSubtypeEnum.OTHER, description);
    }

    @JsonCreator
    public static VisualArtsSubtype fromJson(@JsonProperty(TYPE_FIELD) VisualArtsSubtypeEnum type,
                                             @JsonProperty(DESCRIPTION_FIELD) String description) {
        if (VisualArtsSubtypeEnum.OTHER.equals(type)) {
            return createOther(description);
        }
        return new VisualArtsSubtype(type);
    }

    public static VisualArtsSubtype create(VisualArtsSubtypeEnum type) {
        return new VisualArtsSubtype(type);
    }

    public VisualArtsSubtypeEnum getType() {
        return type;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VisualArtsSubtype)) {
            return false;
        }
        VisualArtsSubtype that = (VisualArtsSubtype) o;
        return getType() == that.getType();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getType());
    }
}
