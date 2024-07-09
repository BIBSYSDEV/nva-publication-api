package no.unit.nva.model.instancetypes.artistic.design;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class ArtisticDesignSubtypeOther extends ArtisticDesignSubtype {
    public static final String DESCRIPTION = "description";
    @JsonProperty(DESCRIPTION)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String description;

    protected ArtisticDesignSubtypeOther(@JsonProperty(TYPE) ArtisticDesignSubtypeEnum type,
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

        if (!(o instanceof ArtisticDesignSubtypeOther)) {
            return false;
        }

        if (!super.equals(o)) {
            return false;
        }

        ArtisticDesignSubtypeOther that = (ArtisticDesignSubtypeOther) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDescription());
    }
}
