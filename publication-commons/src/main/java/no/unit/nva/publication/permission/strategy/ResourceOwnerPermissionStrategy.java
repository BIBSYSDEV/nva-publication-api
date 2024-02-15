package no.unit.nva.publication.permission.strategy;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.UserInstance;

public class ResourceOwnerPermissionStrategy extends PermissionStrategy {

    public ResourceOwnerPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    protected boolean allowsAction(PublicationOperation permission) {
        return switch (permission) {
            case UPDATE, UNPUBLISH -> canModify();
            case DELETE -> canDelete();
            default -> false;
        };
    }

    private boolean canDelete() {
        return isDraft() && isOwner();
    }

    private boolean isDraft() {
        return publication.getStatus().equals(PublicationStatus.DRAFT);
    }

    private boolean canModify() {
        if (isDegree() && !isDraft()) {
            return false;
        }
        return isOwner();
    }

    private Boolean isOwner() {
        return attempt(userInstance::getUsername)
                   .map(username -> UserInstance.fromPublication(publication).getUsername().equals(username))
                   .orElse(fail -> false);
    }
}
