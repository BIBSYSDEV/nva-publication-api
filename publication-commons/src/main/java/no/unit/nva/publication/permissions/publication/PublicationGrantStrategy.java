package no.unit.nva.publication.permissions.publication;

import no.unit.nva.model.PublicationOperation;

public interface PublicationGrantStrategy {
    boolean allowsAction(PublicationOperation permission);
}
