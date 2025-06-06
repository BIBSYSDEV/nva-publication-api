package no.unit.nva.publication.permissions.publication.grant;

import static java.util.Objects.nonNull;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationGrantStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public final class BackendClientGrantStrategy extends PublicationStrategyBase implements PublicationGrantStrategy {

    public BackendClientGrantStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return nonNull(userInstance) && userInstance.isBackendClient();
    }
}
