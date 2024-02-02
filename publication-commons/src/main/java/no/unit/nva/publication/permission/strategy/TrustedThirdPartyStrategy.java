package no.unit.nva.publication.permission.strategy;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public class TrustedThirdPartyStrategy extends PermissionStrategy {

    public TrustedThirdPartyStrategy(Publication publication, UserInstance userInstance, List<AccessRight> accessRights,
                                           URI personCristinId) {
        super(publication, userInstance, accessRights, personCristinId);
    }

    @Override
    public boolean hasPermissionToUpdate() {
        return canModify();
    }

    @Override
    public boolean hasPermissionToDelete() {
        return false;
    }

    @Override
    public boolean hasPermissionToUnpublish() {
        return canModify();
    }

    private boolean canModify() {
        return userInstance.isExternalClient() &&
               attempt(
                   () -> userInstance.getOrganizationUri().equals(publication.getPublisher().getId()))
                   .orElse(fail -> false);
    }
}
