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
        if (isUsersDraft() || userInstance.isExternalClient()) {
            return false; // allow
        }

        if (isProtectedDegreeInstanceType()) {
            if (!hasAccessRight(MANAGE_DEGREE)) {
                return true; // deny
            }
            if (isProtectedDegreeInstanceTypeWithEmbargo() && !hasAccessRight(MANAGE_DEGREE_EMBARGO)) {
                return true; // deny
            }
        }

        return false; // allow
    }

    private boolean isUsersDraft() {
        return isDraft() && isOwner();
    }
}
