package no.unit.nva.publication.permission.strategy.grant;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PermissionStrategy;

public abstract class GrantPermissionStrategy extends PermissionStrategy {

    protected GrantPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    public abstract boolean allowsAction(PublicationOperation permission);

}
