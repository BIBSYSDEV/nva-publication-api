package no.unit.nva.model.instancetypes.artistic.film;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class MovingPictureSubtype {

    public static final String TYPE = "type";
    @JsonProperty(TYPE)
    private final MovingPictureSubtypeEnum type;

    protected MovingPictureSubtype(MovingPictureSubtypeEnum type) {
        this.type = type;
    }

    public static MovingPictureSubtype createOther(String description) {
        return new MovingPictureSubtypeOther(MovingPictureSubtypeEnum.OTHER, description);
    }

    @JsonCreator
    public static MovingPictureSubtype fromJson(@JsonProperty(TYPE) MovingPictureSubtypeEnum type,
                                                @JsonProperty("description") String description) {
        if (MovingPictureSubtypeEnum.OTHER == type) {
            return createOther(description);
        }
        return new MovingPictureSubtype(type);
    }

    public static MovingPictureSubtype create(MovingPictureSubtypeEnum type) {
        return new MovingPictureSubtype(type);
    }

    public MovingPictureSubtypeEnum getType() {
        return type;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getType());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MovingPictureSubtype)) {
            return false;
        }
        MovingPictureSubtype movingPictureSubtype = (MovingPictureSubtype) o;
        return getType() == movingPictureSubtype.getType();
    }
}
