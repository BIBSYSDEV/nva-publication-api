package no.unit.nva.publication.permission.strategy;

import java.net.URI;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public final class PublicationPermissionStrategy {
    private final Set<PermissionStrategy> permissionStrategies;

    private PublicationPermissionStrategy(
        Publication publication,
        UserInstance userInstance,
        List<AccessRight> accessRights,
        URI personCristinId//todo: move to UserInstance?
    ) {
        this.permissionStrategies = Set.of(
            new EditorPermissionStrategy(publication, userInstance, accessRights, personCristinId),
            new CuratorPermissionStrategy(publication, userInstance, accessRights, personCristinId),
            new ContributorPermissionStrategy(publication, userInstance, accessRights, personCristinId),
            new ResourceOwnerPermissionStrategy(publication, userInstance, accessRights, personCristinId),
            new TrustedThirdPartyStrategy(publication, userInstance, accessRights, personCristinId)
        );
    }

    public static PublicationPermissionStrategy fromRequestInfo(Publication publication, UserInstance userInstance, List<AccessRight> accessRights, URI personCristinId) {
        return new PublicationPermissionStrategy(publication, userInstance, accessRights, personCristinId);
    }

    public boolean hasPermissionToDelete() {
        return permissionStrategies.stream()
                   .anyMatch(PermissionStrategy::hasPermissionToDelete);
    }

    public boolean hasPermissionToUpdate() {
        return permissionStrategies.stream()
                   .anyMatch(PermissionStrategy::hasPermissionToUpdate);
    }

    public boolean hasPermissionToUnpublish() {
        return permissionStrategies.stream()
                   .anyMatch(PermissionStrategy::hasPermissionToUnpublish);
    }
}

