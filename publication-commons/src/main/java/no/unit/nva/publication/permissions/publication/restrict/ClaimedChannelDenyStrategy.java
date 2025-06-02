package no.unit.nva.publication.permissions.publication.restrict;

import static no.unit.nva.model.PublicationOperation.APPROVE_FILES;
import static no.unit.nva.model.PublicationOperation.UNPUBLISH;
import static no.unit.nva.model.PublicationOperation.UPDATE;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.permissions.publication.PublicationDenyStrategy;
import no.unit.nva.publication.permissions.publication.PublicationStrategyBase;

public class ClaimedChannelDenyStrategy extends PublicationStrategyBase implements PublicationDenyStrategy {

    public ClaimedChannelDenyStrategy(Resource resource, UserInstance userInstance) {
        super(resource, userInstance);
    }

    @Override
    public boolean deniesAction(PublicationOperation operation) {
        if (isUsersDraft() || userInstance.isExternalClient() || userInstance.isBackendClient()) {
            return false;
        }
        return isDeniedOperation(operation)
               && isPublished()
               && (hasApprovedFiles() || isImportedStudentThesis())
               && isDeniedUserByClaimedChannelWithinScope();
    }

    private static boolean isDeniedOperation(PublicationOperation operation) {
        return UPDATE.equals(operation) || UNPUBLISH.equals(operation) || APPROVE_FILES.equals(operation);
    }

    private boolean isDeniedUserByClaimedChannelWithinScope() {
        return resource.getPrioritizedClaimedPublicationChannelWithinScope()
                   .map(this::channelPolicyDeniesEditing)
                   .orElse(false);
    }

    private boolean channelPolicyDeniesEditing(ClaimedPublicationChannel claim) {
        return switch (claim.getConstraint().editingPolicy()) {
            case OWNER_ONLY -> !userBelongsToPublicationChannelOwner();
            case EVERYONE -> !userRelatesToPublication();
        };
    }
}
