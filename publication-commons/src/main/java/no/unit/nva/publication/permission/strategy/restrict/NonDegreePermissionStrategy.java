package no.unit.nva.publication.permission.strategy.restrict;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;

public class NonDegreePermissionStrategy extends DenyPermissionStrategy {

    public NonDegreePermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean deniesAction(PublicationOperation permission) {
        if (isUsersDraft()) {
            return false; // allow
        }

        if (isDegree()) {
            if (!hasAccessRight(MANAGE_DEGREE)) {
                return true; // deny
            }
            if (isEmbargoDegree() && !hasAccessRight(MANAGE_DEGREE_EMBARGO)) {
                return true; // deny
            }
        }

        return false; // allow
    }

    private boolean isUsersDraft() {
        return isDraft() && isOwner();
    }
}
