package no.unit.nva.model.instancetypes.artistic.music;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class MusicMediaSubtype {

    public static final String TYPE_FIELD = "type";
    public static final String DESCRIPTION_FIELD = "description";
    private final MusicMediaType type;

    public MusicMediaSubtype(@JsonProperty(TYPE_FIELD) MusicMediaType type) {
        this.type = type;
    }

    public static MusicMediaSubtype createOther(String description) {
        return new MusicMediaSubtypeOther(MusicMediaType.OTHER, description);
    }

    @JsonCreator
    public static MusicMediaSubtype fromJson(@JsonProperty(TYPE_FIELD) MusicMediaType type,
                                             @JsonProperty(DESCRIPTION_FIELD) String description) {
        if (MusicMediaType.OTHER.equals(type)) {
            return createOther(description);
        }
        return new MusicMediaSubtype(type);
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MusicMediaSubtype that = (MusicMediaSubtype) o;
        return getType() == that.getType();
    }

    public MusicMediaType getType() {
        return type;
    }
}
