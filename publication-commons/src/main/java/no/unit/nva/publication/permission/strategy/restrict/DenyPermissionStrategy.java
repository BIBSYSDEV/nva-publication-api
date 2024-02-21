package no.unit.nva.publication.permission.strategy.restrict;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PermissionStrategy;

public abstract class DenyPermissionStrategy extends PermissionStrategy {

    protected DenyPermissionStrategy(Publication publication, UserInstance userInstance, UriRetriever uriRetriever) {
        super(publication, userInstance, uriRetriever);
    }

    public abstract boolean deniesAction(PublicationOperation permission);
}
