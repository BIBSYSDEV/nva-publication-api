package no.unit.nva.publication.permission.strategy;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;

public class TrustedThirdPartyStrategy extends PermissionStrategy {

    public TrustedThirdPartyStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    protected boolean hasPermission(PublicationAction permission) {
        return switch (permission) {
            case UPDATE, UNPUBLISH -> canModify();
            default -> false;
        };
    }

    private boolean canModify() {
        return userInstance.isExternalClient() &&
               attempt(
                   () -> userInstance.getOrganizationUri().equals(publication.getPublisher().getId()))
                   .orElse(fail -> false);
    }
}
