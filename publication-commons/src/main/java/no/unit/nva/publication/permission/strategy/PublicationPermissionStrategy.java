package no.unit.nva.publication.permission.strategy;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.exceptions.UnauthorizedException;

public final class PublicationPermissionStrategy {

    private final Set<PermissionStrategy> permissionStrategies;
    private final UserInstance userInstance;
    private final Publication publication;

    private PublicationPermissionStrategy(
        Publication publication,
        UserInstance userInstance
    ) {
        this.userInstance = userInstance;
        this.publication = publication;
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

    public boolean allowsAction(PublicationOperation permission) {
        return permissionStrategies.stream()
                   .anyMatch(strategy -> strategy.allowsAction(permission));
    }

    public Set<PublicationOperation> getAllAllowedActions() {
        return Arrays.stream(PublicationOperation.values())
                   .filter(this::allowsAction)
                   .collect(Collectors.toSet());
    }

    public void authorize(PublicationOperation requestedPermission) throws UnauthorizedException {
        if (!allowsAction(requestedPermission)) {
            throw new UnauthorizedException(
                String.format("Unauthorized: %s is not allowed to perform %s on %s", userInstance.getUsername(),
                              requestedPermission, publication.getIdentifier()));
        }
    }
}

