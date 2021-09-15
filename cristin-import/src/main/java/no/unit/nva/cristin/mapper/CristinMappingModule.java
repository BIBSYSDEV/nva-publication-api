package no.unit.nva.cristin.mapper;

import java.util.Optional;

/**
 * Class containing common functionality for the different modules implementing the mapping logic of Cristin entries
 * to NVA entries.
 */
public class CristinMappingModule {

    protected final CristinObject cristinObject;

    public CristinMappingModule(CristinObject cristinObject) {
        this.cristinObject = cristinObject;
    }

    protected CristinJournalPublication extractCristinJournalPublication() {
        return Optional.ofNullable(cristinObject)
            .map(CristinObject::getJournalPublication)
            .orElse(null);
    }
}
