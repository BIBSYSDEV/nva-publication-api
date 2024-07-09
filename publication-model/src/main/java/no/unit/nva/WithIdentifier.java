package no.unit.nva;

import no.unit.nva.identifiers.SortableIdentifier;

public interface WithIdentifier extends PublicationBase {

    SortableIdentifier getIdentifier();

    void setIdentifier(SortableIdentifier identifier);

}
