package no.unit.nva.cristin;

import java.util.Set;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Publication;

public class CristinMapper {

    private final CristinObject cristinObject;

    public CristinMapper(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    public Publication generatePublication() {
        return new Publication.Builder()
                   .withAdditionalIdentifiers(Set.of(extractIdentifier()))
                   .build();
    }

    private AdditionalIdentifier extractIdentifier() {
        return new AdditionalIdentifier(CristinObject.IDENTIFIER_ORIGIN, cristinObject.getId());
    }
}
