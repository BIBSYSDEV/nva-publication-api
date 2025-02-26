package no.unit.nva.publication.permissions.publication;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.grant.BackendClientGrantStrategy;
import no.unit.nva.publication.permissions.publication.grant.ContributorGrantStrategy;
import no.unit.nva.publication.permissions.publication.grant.CuratorGrantStrategy;
import no.unit.nva.publication.permissions.publication.grant.EditorGrantStrategy;
import no.unit.nva.publication.permissions.publication.grant.ResourceOwnerGrantStrategy;
import no.unit.nva.publication.permissions.publication.grant.TrustedThirdPartyGrantStrategy;
import no.unit.nva.publication.permissions.publication.restrict.DegreeDenyStrategy;
import no.unit.nva.publication.permissions.publication.restrict.DeletedUplaodDenyStrategy;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationPermissions {

    private static final Logger logger = LoggerFactory.getLogger(PublicationPermissions.class);
    public static final String COMMA_DELIMITER = ", ";
    private final Set<PublicationGrantStrategy> grantStrategies;
    private final Set<PublicationDenyStrategy> denyStrategies;
    private final UserInstance userInstance;
    private final Publication publication;

    public PublicationPermissions(
        Publication publication,
        UserInstance userInstance) {
        this.userInstance = userInstance;
        this.publication = publication;
        this.grantStrategies = Set.of(
            new EditorGrantStrategy(publication, userInstance),
            new CuratorGrantStrategy(publication, userInstance),
            new ContributorGrantStrategy(publication, userInstance),
            new ResourceOwnerGrantStrategy(publication, userInstance),
            new TrustedThirdPartyGrantStrategy(publication, userInstance),
            new BackendClientGrantStrategy(publication, userInstance)
        );
        this.denyStrategies = Set.of(
            new DegreeDenyStrategy(publication, userInstance),
            new DeletedUplaodDenyStrategy(publication, userInstance)
        );
    }

    public static PublicationPermissions create(Publication publication, UserInstance userInstance) {
        return new PublicationPermissions(publication, userInstance);
    }

    public boolean isCuratorOnPublication() {
        return isCurator()
               && findDenials(PublicationOperation.UPDATE).isEmpty();
    }

    public boolean isPublishingCuratorOnPublication() {
        return isPublishingCurator()
               && findDenials(PublicationOperation.UPDATE_FILES).isEmpty();
    }

    private boolean isPublishingCurator() {
        return findAllowances(PublicationOperation.UPDATE_FILES)
                   .stream()
                   .anyMatch(CuratorGrantStrategy.class::isInstance);
    }

    private boolean isCurator() {
        return findAllowances(PublicationOperation.UPDATE)
                   .stream()
                   .anyMatch(CuratorGrantStrategy.class::isInstance);
    }

    public boolean allowsAction(PublicationOperation permission) {
        return !findAllowances(permission).isEmpty()
               && findDenials(permission).isEmpty();
    }

    public Set<PublicationOperation> getAllAllowedActions() {
        return Arrays.stream(PublicationOperation.values())
                   .filter(this::allowsAction)
                   .collect(Collectors.toSet());
    }

    public void authorize(PublicationOperation requestedPermission) throws UnauthorizedException {
        validateDenyStrategiesRestrictions(requestedPermission);
        validateGrantStrategies(requestedPermission);
    }

    private List<PublicationGrantStrategy> findAllowances(PublicationOperation permission) {
        return grantStrategies.stream()
                   .filter(strategy -> strategy.allowsAction(permission))
                   .toList();
    }

    private List<PublicationDenyStrategy> findDenials(PublicationOperation permission) {
        return denyStrategies.stream()
                   .filter(strategy -> strategy.deniesAction(permission))
                   .toList();
    }

    private void validateDenyStrategiesRestrictions(PublicationOperation requestedPermission)
        throws UnauthorizedException {
        var strategies = findDenials(requestedPermission).stream()
                                     .map(PublicationDenyStrategy::getClass)
                                     .map(Class::getSimpleName)
                                     .toList();

        if (!strategies.isEmpty()) {
            logger.info("User {} was denied access {} on publication {} from strategies {}",
                        userInstance.getUsername(),
                        requestedPermission,
                        publication.getIdentifier(),
                        String.join(COMMA_DELIMITER, strategies));

            throw new UnauthorizedException(formatUnauthorizedMessage(requestedPermission));
        }
    }

    private void validateGrantStrategies(PublicationOperation requestedPermission) throws UnauthorizedException {
        var strategies = findAllowances(requestedPermission).stream()
                                  .map(PublicationGrantStrategy::getClass)
                                  .map(Class::getSimpleName)
                                  .toList();

        if (strategies.isEmpty()) {
            throw new UnauthorizedException(formatUnauthorizedMessage(requestedPermission));
        }

        logger.info("User {} was allowed {} on publication {} from strategies {}",
                    userInstance.getUsername(),
                    requestedPermission,
                    publication.getIdentifier(),
                    String.join(COMMA_DELIMITER, strategies));
    }

    private String formatUnauthorizedMessage(PublicationOperation requestedPermission) {
        return String.format("Unauthorized: %s is not allowed to perform %s on %s", userInstance.getUsername(),
                             requestedPermission, publication.getIdentifier());
    }

}

