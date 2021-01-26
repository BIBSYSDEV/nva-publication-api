package no.unit.nva.publication.storage.model.daos;

import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.core.JacocoGenerated;

public class UniqueDoiRequestEntry extends UniquenessEntry {

    private static final String TYPE = "DoiRequestEntry" + DatabaseConstants.KEY_FIELDS_DELIMITER;

    @JacocoGenerated
    public UniqueDoiRequestEntry() {
        super();
    }

    public UniqueDoiRequestEntry(String identifier) {
        super(identifier);
    }

    @Override
    protected String getType() {
        return TYPE;
    }
}
