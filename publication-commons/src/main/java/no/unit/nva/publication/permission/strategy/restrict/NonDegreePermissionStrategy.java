package no.unit.nva.publication.permission.strategy.restrict;

import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

public class NonDegreePermissionStrategy extends DenyPermissionStrategy {

    public NonDegreePermissionStrategy(Publication publication, UserInstance userInstance, ResourceService resourceService) {
        super(publication, userInstance, resourceService);
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
            if (!userIsFromSameInstitutionAsPublication()) {
                return true; // deny
            }
        }

        return false; // allow
    }

    private boolean isUsersDraft() {
        return isDraft() && isOwner();
    }
}
