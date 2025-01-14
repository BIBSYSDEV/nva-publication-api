package no.unit.nva.publication.permissions.publication;

import no.unit.nva.model.PublicationOperation;

public interface PublicationDenyStrategy  {
    boolean deniesAction(PublicationOperation permission);
}
