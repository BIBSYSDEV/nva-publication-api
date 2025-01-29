package no.unit.nva.publication.permissions.publication.grant;

import static java.util.Objects.isNull;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class BackendClientGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public BackendClientGrantStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    public boolean allowsAction(PublicationOperation permission) {
        if (isNull(userInstance)) {
            return false;
        }

        return userInstance.isBackendClient();
    }
}
