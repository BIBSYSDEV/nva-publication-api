package no.unit.nva.publication.permission.strategy.grant;

import static nva.commons.core.attempt.Try.attempt;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;

public class TrustedThirdPartyStrategy extends GrantPermissionStrategy {

    public TrustedThirdPartyStrategy(Publication publication, UserInstance userInstance) {
        super(publication, userInstance);
    }

    @Override
    public boolean allowsAction(PublicationOperation permission) {
        return switch (permission) {
            case UPDATE, UNPUBLISH -> canModify();
            default -> false;
        };
    }

    private boolean canModify() {
        return userInstance.isExternalClient() &&
               attempt(
                   () -> userInstance.getCustomerId().equals(publication.getPublisher().getId()))
                   .orElse(fail -> false);
    }
}