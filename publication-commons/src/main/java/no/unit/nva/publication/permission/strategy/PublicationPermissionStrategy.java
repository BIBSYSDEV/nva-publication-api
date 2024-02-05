package no.unit.nva.publication.permission.strategy;

import java.util.Set;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.UserInstance;

public final class PublicationPermissionStrategy {
    private final Set<PermissionStrategy> permissionStrategies;

    private PublicationPermissionStrategy(
        Publication publication,
        UserInstance userInstance
    ) {
        this.permissionStrategies = Set.of(
            new EditorPermissionStrategy(publication, userInstance),
            new CuratorPermissionStrategy(publication, userInstance),
            new ContributorPermissionStrategy(publication, userInstance),
            new ResourceOwnerPermissionStrategy(publication, userInstance),
            new TrustedThirdPartyStrategy(publication, userInstance)
        );
    }

    public static PublicationPermissionStrategy create(Publication publication, UserInstance userInstance) {
        return new PublicationPermissionStrategy(publication, userInstance);
    }

    public boolean hasPermission(PublicationPermission permission) {
        return permissionStrategies.stream()
                   .anyMatch(p -> p.hasPermission(permission));
    }
}

