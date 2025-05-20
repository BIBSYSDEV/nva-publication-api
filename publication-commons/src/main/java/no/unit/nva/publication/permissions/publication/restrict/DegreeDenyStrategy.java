package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;
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
    public boolean deniesAction(PublicationOperation operation) {
        if (isUsersDraft() || userInstance.isExternalClient() || userInstance.isBackendClient() || isRelatedEditor()) {
            return PASS;
        }

        return isDeniedOperation(operation) && isProtectedDegreeInstanceType() && handleDegree();
    }

    private boolean isRelatedEditor() {
        return userInstance.getAccessRights().contains(MANAGE_RESOURCES_ALL) && userRelatesToPublication();
    }

    private static boolean isDeniedOperation(PublicationOperation operation) {
        return UPDATE.equals(operation) || UNPUBLISH.equals(operation);
    }

    private boolean handleDegree() {
        if (hasApprovedFiles()) {
            return approvedFileStrategy();
        } else {
            return nonApprovedFileStrategy();
        }
    }

    private boolean approvedFileStrategy() {
        if (!hasAccessRight(MANAGE_DEGREE)) {
            return DENY;
        }
        if (resource.getPrioritizedClaimedPublicationChannelWithinScope().isEmpty()) {
            return !userIsFromSameInstitutionAsPublicationOwner();
        } // else: ClaimedChannelDenyStrategy takes care of denying by channel claim
        return PASS;
    }

    private boolean nonApprovedFileStrategy() {
        if (!userRelatesToPublicationThroughPublicationOwnerOrCuratingInstitution()) {
            return DENY;
        }
        return PASS;
    }
}
