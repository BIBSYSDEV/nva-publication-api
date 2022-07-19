package no.unit.nva.publication.model.business;

import no.unit.nva.identifiers.SortableIdentifier;

public interface WithIdentifier {
    
    SortableIdentifier getIdentifier();
    
    void setIdentifier(SortableIdentifier identifier);
}
