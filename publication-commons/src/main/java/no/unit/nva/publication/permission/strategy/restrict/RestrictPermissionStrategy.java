package no.unit.nva.publication.permission.strategy.restrict;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PermissionStrategy;

public abstract class RestrictPermissionStrategy extends PermissionStrategy {

    protected RestrictPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    public abstract boolean deniesAction(PublicationOperation permission);
}
