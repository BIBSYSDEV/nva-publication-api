package no.unit.nva.publication.permission.strategy;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.grant.ContributorPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.CuratorPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.EditorPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.GrantPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.ResourceOwnerPermissionStrategy;
import no.unit.nva.publication.permission.strategy.grant.TrustedThirdPartyStrategy;
import no.unit.nva.publication.permission.strategy.restrict.NonDegreePermissionStrategy;
import no.unit.nva.publication.permission.strategy.restrict.DenyPermissionStrategy;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublicationPermissionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(PublicationPermissionStrategy.class);
    public static final String COMMA_DELIMITER = ", ";
    private final Set<GrantPermissionStrategy> grantStrategies;
    private final Set<DenyPermissionStrategy> denyStrategies;
    private final UserInstance userInstance;
    private final Publication publication;

    public PublicationPermissionStrategy(
        Publication publication,
        UserInstance userInstance,
        UriRetriever uriRetriever
    ) {
        this.userInstance = userInstance;
        this.publication = publication;
        this.grantStrategies = Set.of(
            new EditorPermissionStrategy(publication, userInstance, uriRetriever),
            new CuratorPermissionStrategy(publication, userInstance, uriRetriever),
            new ContributorPermissionStrategy(publication, userInstance, uriRetriever),
            new ResourceOwnerPermissionStrategy(publication, userInstance, uriRetriever),
            new TrustedThirdPartyStrategy(publication, userInstance, uriRetriever)
        );
        this.denyStrategies = Set.of(
            new NonDegreePermissionStrategy(publication, userInstance, uriRetriever)
        );
    }

    public static PublicationPermissionStrategy create(Publication publication, UserInstance userInstance,
                                                       UriRetriever uriRetriever) {
        return new PublicationPermissionStrategy(publication, userInstance, uriRetriever);
    }

    public boolean isCuratorOnPublication() {
        return isCurator()
               && findDenials(PublicationOperation.UPDATE).isEmpty();
    }

    private boolean isCurator() {
        return findAllowances(PublicationOperation.UPDATE)
                   .stream()
                   .anyMatch(grant -> grant instanceof CuratorPermissionStrategy);
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

    private List<GrantPermissionStrategy> findAllowances(PublicationOperation permission) {
        return grantStrategies.stream()
                   .filter(strategy -> strategy.allowsAction(permission))
                   .toList();
    }

    private List<DenyPermissionStrategy> findDenials(PublicationOperation permission) {
        return denyStrategies.stream()
                   .filter(strategy -> strategy.deniesAction(permission))
                   .toList();
    }

    private void validateDenyStrategiesRestrictions(PublicationOperation requestedPermission)
        throws UnauthorizedException {
        var strategies = findDenials(requestedPermission).stream()
                                     .map(DenyPermissionStrategy::getClass)
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
                                  .map(GrantPermissionStrategy::getClass)
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

