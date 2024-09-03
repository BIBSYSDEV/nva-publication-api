package no.unit.nva.publication.permission.strategy.grant;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;

public abstract class GrantPermissionStrategy extends PermissionStrategy {

    protected GrantPermissionStrategy(Publication publication, UserInstance userInstance, ResourceService resourceService) {
        super(publication, userInstance, resourceService);
    }

    public abstract boolean allowsAction(PublicationOperation permission);

}
