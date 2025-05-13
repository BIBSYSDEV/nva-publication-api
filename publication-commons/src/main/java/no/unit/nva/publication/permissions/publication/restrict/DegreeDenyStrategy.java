package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationOperation.PARTIAL_UPDATE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationDenyStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public class DegreeDenyStrategy extends PublicationStrategyBase implements PublicationDenyStrategy {

    private static final boolean DENY = true;
    private static final boolean PASS = false;

    public DegreeDenyStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean deniesAction(PublicationOperation permission) {
        if (isUsersDraft() || userInstance.isExternalClient() || userInstance.isBackendClient()) {
            return PASS;
        }

        if (isProtectedDegreeInstanceType()) {
            if (PARTIAL_UPDATE.equals(permission)) {
                return !userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution();
            }
            if (isProtectedDegreeInstanceTypeWithEmbargo() && !hasAccessRight(MANAGE_DEGREE_EMBARGO)) {
                return DENY;
            }
            if (hasApprovedFiles()) {
                return approvedFileStrategy();
            } else {
                return nonOpenFileStrategy();
            }
        }

        return PASS;
    }

    private boolean approvedFileStrategy() {
        if (!hasAccessRight(MANAGE_DEGREE)) {
            return DENY;
        }
        // TODO: Implement when channelClaim is available in publication object
        //        if (!userIsFromSameInstitutionAsPublicationOwner()) {
        //            return DENY;
        //        }
        return PASS;
    }

    private boolean nonOpenFileStrategy() {
        if (!userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution()) {
            return DENY;
        }
        if (!userIsFromSameInstitutionAsPublicationOwner()) {
            return DENY;
        }
        return PASS;
    }
}
