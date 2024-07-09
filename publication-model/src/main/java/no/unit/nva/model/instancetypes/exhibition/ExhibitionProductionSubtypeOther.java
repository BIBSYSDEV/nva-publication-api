package no.unit.nva.model.instancetypes.exhibition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class ExhibitionProductionSubtypeOther extends ExhibitionProductionSubtype {
    public static final String TYPE_FIELD = "type";
    public static final String DESCRIPTION_FIELD = "description";
    private final String description;

    @JsonCreator
    public ExhibitionProductionSubtypeOther(@JsonProperty(TYPE_FIELD) ExhibitionProductionSubtypeEnum type,
                                            @JsonProperty(DESCRIPTION_FIELD) String description) {
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
        if (!(o instanceof ExhibitionProductionSubtypeOther)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ExhibitionProductionSubtypeOther that = (ExhibitionProductionSubtypeOther) o;
        return Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDescription());
    }
}
