package no.unit.nva.model.instancetypes.artistic.design;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class ArtisticDesignSubtype {
    public static final String TYPE = "type";
    @JsonProperty(TYPE)
    private final ArtisticDesignSubtypeEnum type;

    public static ArtisticDesignSubtype createOther(String description) {
        return new ArtisticDesignSubtypeOther(ArtisticDesignSubtypeEnum.OTHER, description);
    }

    @JsonCreator
    public static ArtisticDesignSubtype fromJson(@JsonProperty(TYPE) ArtisticDesignSubtypeEnum type,
                                                 @JsonProperty("description") String description) {
        if (ArtisticDesignSubtypeEnum.OTHER.equals(type)) {
            return createOther(description);
        }
        return new ArtisticDesignSubtype(type);
    }

    public static ArtisticDesignSubtype create(ArtisticDesignSubtypeEnum type) {
        return new ArtisticDesignSubtype(type);
    }

    protected ArtisticDesignSubtype(ArtisticDesignSubtypeEnum type) {
        this.type = type;
    }

    public ArtisticDesignSubtypeEnum getType() {
        return type;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArtisticDesignSubtype)) {
            return false;
        }
        ArtisticDesignSubtype artisticDesignSubtype = (ArtisticDesignSubtype) o;
        return getType() == artisticDesignSubtype.getType();
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getType());
    }
}
