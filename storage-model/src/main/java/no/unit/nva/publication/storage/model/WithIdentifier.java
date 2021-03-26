package no.unit.nva.publication.storage.model;

import no.unit.nva.identifiers.SortableIdentifier;


public interface WithIdentifier {

    SortableIdentifier getIdentifier();

    void setIdentifier(SortableIdentifier identifier);
}
