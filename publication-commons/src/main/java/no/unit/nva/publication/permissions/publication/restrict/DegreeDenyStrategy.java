package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationOperation.PARTIAL_UPDATE;
import static no.unit.nva.model.PublicationOperation.UPLOAD_FILE;
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
            return switch (permission) {
                case UPLOAD_FILE, PARTIAL_UPDATE, READ_HIDDEN_FILES -> !userRelatesToPublication();
                default -> handle();
            };
        }

        return PASS;
    }

    private boolean handle() {
        if (isProtectedDegreeInstanceTypeWithEmbargo() && !hasAccessRight(MANAGE_DEGREE_EMBARGO)) { // SKAL FJERNES
            return DENY;
        }                                                                                           // SKAL FJERNES
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
