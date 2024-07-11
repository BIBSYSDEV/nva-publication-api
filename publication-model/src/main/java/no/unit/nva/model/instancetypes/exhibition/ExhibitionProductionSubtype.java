package no.unit.nva.model.instancetypes.exhibition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class ExhibitionProductionSubtype {


    public static final String TYPE = "type";
    public static final String DESCRIPTION = "description";
    @JsonProperty(TYPE)
    private final ExhibitionProductionSubtypeEnum subtype;

    public ExhibitionProductionSubtype(ExhibitionProductionSubtypeEnum subtype) {
        this.subtype = subtype;
    }

    public static ExhibitionProductionSubtype createOther(String description) {
        return new ExhibitionProductionSubtypeOther(ExhibitionProductionSubtypeEnum.OTHER, description);
    }

    @JsonCreator
    public static ExhibitionProductionSubtype fromJson(@JsonProperty(TYPE) ExhibitionProductionSubtypeEnum type,
                                               @JsonProperty(DESCRIPTION) String description) {
        if (ExhibitionProductionSubtypeEnum.OTHER.equals(type)) {
            return createOther(description);
        }
        return new ExhibitionProductionSubtype(type);
    }

    public static ExhibitionProductionSubtype create(ExhibitionProductionSubtypeEnum type) {
        return new ExhibitionProductionSubtype(type);
    }

    public ExhibitionProductionSubtypeEnum getSubtype() {
        return subtype;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExhibitionProductionSubtype)) {
            return false;
        }
        ExhibitionProductionSubtype that = (ExhibitionProductionSubtype) o;
        return getSubtype() == that.getSubtype();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getSubtype());
    }
}
