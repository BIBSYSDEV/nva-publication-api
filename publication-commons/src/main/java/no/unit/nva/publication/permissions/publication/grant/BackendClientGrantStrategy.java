package no.unit.nva.publication.permissions.publication.grant;

import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;

public class BackendClientGrantStrategy extends PublicationGrantStrategy {

    public BackendClientGrantStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return userInstance.isBackendClient();
    }
}
