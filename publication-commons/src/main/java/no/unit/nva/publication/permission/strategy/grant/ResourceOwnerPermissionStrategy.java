package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.UserInstance;

public class ResourceOwnerPermissionStrategy extends GrantPermissionStrategy {

    public ResourceOwnerPermissionStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return switch (permission) {
            case UPDATE, UNPUBLISH -> canModify();
            case DELETE -> canDelete();
            default -> false;
        };
    }

    private boolean canDelete() {
        return isDraft() && isOwner();
    }

    private boolean canModify() {
        return isOwner();
    }
}
