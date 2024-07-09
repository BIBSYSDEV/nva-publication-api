package no.unit.nva.model.instancetypes.artistic.performingarts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class PerformingArtsSubtype {
    public static final String TYPE = "type";
    public static final String DESCRIPTION = "description";
    @JsonProperty(TYPE)
    private final PerformingArtsSubtypeEnum type;

    public static PerformingArtsSubtype createOther(String description) {
        return new PerformingArtsSubtypeOther(PerformingArtsSubtypeEnum.OTHER, description);
    }

    @JacocoGenerated
    @JsonCreator
    public static PerformingArtsSubtype fromJson(@JsonProperty(TYPE) PerformingArtsSubtypeEnum type,
                                                 @JsonProperty(DESCRIPTION) String description) {
        if (PerformingArtsSubtypeEnum.OTHER == type) {
            return createOther(description);
        }
        return new PerformingArtsSubtype(type);
    }

    public static PerformingArtsSubtype create(PerformingArtsSubtypeEnum type) {
        return new PerformingArtsSubtype(type);
    }

    protected PerformingArtsSubtype(PerformingArtsSubtypeEnum type) {
        this.type = type;
    }

    public PerformingArtsSubtypeEnum getType() {
        return type;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PerformingArtsSubtype)) {
            return false;
        }
        PerformingArtsSubtype that = (PerformingArtsSubtype) o;
        return getType() == that.getType();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getType());
    }
}
