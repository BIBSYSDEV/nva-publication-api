package no.unit.nva.cristin.mapper;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;
import nva.commons.core.JacocoGenerated;

public interface PublicationInstanceBuilder {

    String ERROR_PARSING_SECONDARY_CATEGORY = "Error parsing secondary category";

    PublicationInstance<? extends Pages> build();

    @JacocoGenerated
    default UnsupportedOperationException unknownSecondaryCategory() {
        return new UnsupportedOperationException(ERROR_PARSING_SECONDARY_CATEGORY);
    }
}
