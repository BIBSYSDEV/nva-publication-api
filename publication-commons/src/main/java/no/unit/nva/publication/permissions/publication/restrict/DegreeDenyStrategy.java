package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationOperation.UPDATE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
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

        return UPDATE.equals(permission)
            && isProtectedDegreeInstanceType()
            && handleUpdate();
    }

    private boolean handleUpdate() {
        if (hasApprovedFiles()) {
            return approvedFileStrategy();
        } else {
            return nonApprovedFileStrategy();
        }
    }

    private boolean approvedFileStrategy() {
        if (!hasAccessRight(MANAGE_DEGREE)) { // Allow editor to unpublish all degrees.
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
