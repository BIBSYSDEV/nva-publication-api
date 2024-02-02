package no.unit.nva.publication.permission.strategy;

import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public class ResourceOwnerPermissionStrategy extends PermissionStrategy {

    public ResourceOwnerPermissionStrategy(Publication publication, UserInstance userInstance, List<AccessRight> accessRights,
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
        if (isDegree() && !publication.getStatus().equals(PublicationStatus.DRAFT)) {
            return false;
        }
        return attempt(userInstance::getUsername)
                   .map(username -> UserInstance.fromPublication(publication).getUsername().equals(username))
                   .orElse(fail -> false);
    }
}
