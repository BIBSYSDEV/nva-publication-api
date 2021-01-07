package no.unit.nva.publication.storage.model;

import no.unit.nva.PublicationBase;
import no.unit.nva.publication.identifiers.SortableIdentifier;

public interface WithIdentifier extends PublicationBase {

    SortableIdentifier getIdentifier();

    void setIdentifier(SortableIdentifier identifier);
}
