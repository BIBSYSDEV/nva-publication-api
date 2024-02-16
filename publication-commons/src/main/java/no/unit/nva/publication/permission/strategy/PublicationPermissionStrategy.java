package no.unit.nva.publication.permission.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.grant.ContributorPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.CuratorPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.EditorPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.GrantPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.ResourceOwnerPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.TrustedThirdPartyStrategy;
import no.unit.nva.publication.permission.strategy.restrict.NonDegreePermissionStrategy;
import no.unit.nva.publication.permission.strategy.restrict.RestrictPermissionStrategy;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PublicationPermissionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(PublicationPermissionStrategy.class);
    public static final String COMMA_DELIMITER = ", ";
    private final Set<GrantPermissionStrategy> grantStrategies;
    private final Set<RestrictPermissionStrategy> restrictStrategies;
    private final UserInstance userInstance;
    private final Publication publication;

    private PublicationPermissionStrategy(
        Publication publication,
        UserInstance userInstance
    ) {
        this.userInstance = userInstance;
        this.publication = publication;
        this.grantStrategies = Set.of(
            new EditorPermissionStrategy(publication, userInstance),
            new CuratorPermissionStrategy(publication, userInstance),
            new ContributorPermissionStrategy(publication, userInstance),
            new ResourceOwnerPermissionStrategy(publication, userInstance),
            new TrustedThirdPartyStrategy(publication, userInstance)
        );
        this.restrictStrategies = Set.of(
            new NonDegreePermissionStrategy(publication, userInstance)
        );
    }

    public static PublicationPermissionStrategy create(Publication publication, UserInstance userInstance) {
        return new PublicationPermissionStrategy(publication, userInstance);
    }

    public boolean allowsAction(PublicationOperation permission) {
        return !findStrategiesAllowingOperation(permission).isEmpty()
               && findStrategiesRestrictingOperation(permission).isEmpty();
    }

    private List<GrantPermissionStrategy> findStrategiesAllowingOperation(PublicationOperation permission) {
        return grantStrategies.stream()
                   .filter(strategy -> strategy.allowsAction(permission))
                   .toList();
    }

    private List<RestrictPermissionStrategy> findStrategiesRestrictingOperation(PublicationOperation permission) {
        return restrictStrategies.stream()
                   .filter(strategy -> strategy.deniesAction(permission))
                   .toList();
    }

    public Set<PublicationOperation> getAllAllowedActions() {
        return Arrays.stream(PublicationOperation.values())
                   .filter(this::allowsAction)
                   .collect(Collectors.toSet());
    }

    public void authorize(PublicationOperation requestedPermission) throws UnauthorizedException {
        authorizeRestrictions(requestedPermission);
        authorizeGrants(requestedPermission);
    }

    private void authorizeRestrictions(PublicationOperation requestedPermission) throws UnauthorizedException {
        var restrictStrategies = findStrategiesRestrictingOperation(requestedPermission).stream()
                                     .map(RestrictPermissionStrategy::getClass)
                                     .map(Class::getSimpleName)
                                     .toList();

        if (!restrictStrategies.isEmpty()) {
            logger.info("User {} was denies access {} on publication {} from strategies {}",
                        userInstance.getUsername(),
                        requestedPermission,
                        publication.getIdentifier(),
                        String.join(COMMA_DELIMITER, restrictStrategies));

            throw new UnauthorizedException(formatUnauthorizedMessage(requestedPermission));
        }
    }



    private void authorizeGrants(PublicationOperation requestedPermission) throws UnauthorizedException {
        var grantStrategies = findStrategiesAllowingOperation(requestedPermission).stream()
                                  .map(GrantPermissionStrategy::getClass)
                                  .map(Class::getSimpleName)
                                  .toList();

        if (grantStrategies.isEmpty()) {
            throw new UnauthorizedException(formatUnauthorizedMessage(requestedPermission));
        }

        logger.info("User {} was allowed {} on publication {} from strategies {}",
                    userInstance.getUsername(),
                    requestedPermission,
                    publication.getIdentifier(),
                    String.join(COMMA_DELIMITER, grantStrategies));
    }

    private String formatUnauthorizedMessage(PublicationOperation requestedPermission) {
        return String.format("Unauthorized: %s is not allowed to perform %s on %s", userInstance.getUsername(),
                             requestedPermission, publication.getIdentifier());
    }

}

