package no.unit.nva.publication.permission.strategy.restrict;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;

public class NonDegreePermissionStrategy extends DenyPermissionStrategy {

    public NonDegreePermissionStrategy(Publication publication, UserInstance userInstance, UriRetriever uriRetriever) {
        super(publication, userInstance, uriRetriever);
    }

    @Override
    public boolean deniesAction(PublicationOperation permission) {
        if (!isDegree() || isUsersDraft()) {
            return false;
        }

        return !hasAccessRight(MANAGE_DEGREE) && !userInstance.isExternalClient();
    }

    private boolean isUsersDraft() {
        return isDraft() && isOwner();
    }
}
