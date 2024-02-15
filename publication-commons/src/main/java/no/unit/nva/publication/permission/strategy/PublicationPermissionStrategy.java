package no.unit.nva.publication.permission.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PublicationPermissionStrategy {
    private static final Logger logger = LoggerFactory.getLogger(PublicationPermissionStrategy.class);
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
        return !findStrategiesAllowingOperation(permission).isEmpty();
    }

    private List<PermissionStrategy> findStrategiesAllowingOperation(PublicationOperation permission) {
        return permissionStrategies.stream()
                   .filter(strategy -> strategy.allowsAction(permission))
                   .toList();
    }

    private List<String> findStrategiesNamesAllowingOperation(PublicationOperation permission) {
        return findStrategiesAllowingOperation(permission).stream()
                   .map(PermissionStrategy::getClass)
                   .map(Class::getSimpleName)
                   .toList();
    }

    public Set<PublicationOperation> getAllAllowedActions() {
        return Arrays.stream(PublicationOperation.values())
                   .filter(this::allowsAction)
                   .collect(Collectors.toSet());
    }

    public void authorize(PublicationOperation requestedPermission) throws UnauthorizedException {
        var strategies = findStrategiesNamesAllowingOperation(requestedPermission);

        if (strategies.isEmpty()) {
            throw new UnauthorizedException(
                String.format("Unauthorized: %s is not allowed to perform %s on %s", userInstance.getUsername(),
                              requestedPermission, publication.getIdentifier()));
        }

        logger.info("User {} was allowed {} on publication {} from strategies {}",
                    userInstance.getUsername(),
                    requestedPermission,
                    publication.getIdentifier(),
                    String.join(", ", strategies));
    }
}

