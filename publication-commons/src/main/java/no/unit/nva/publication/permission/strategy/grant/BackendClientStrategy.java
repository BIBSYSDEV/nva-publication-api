package no.unit.nva.publication.permission.strategy.grant;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;

public class BackendClientStrategy extends GrantPermissionStrategy {

    public BackendClientStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return userInstance.isBackendClient();
    }
}
