package no.unit.nva.publication.permissions.ticket;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.permissions.ticket.deny.ClaimedChannelTicketDenyStrategy;
import no.unit.nva.publication.permissions.ticket.deny.FinalizedTicketDenyStrategy;
import no.unit.nva.publication.permissions.ticket.grant.ApproveTicketGrantStrategy;
import no.unit.nva.publication.permissions.ticket.grant.ReadTicketGrantStrategy;
import no.unit.nva.publication.permissions.ticket.grant.TransferTicketGrantStrategy;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketPermissions {

    private static final Logger logger = LoggerFactory.getLogger(TicketPermissions.class);
    private static final String COMMA_DELIMITER = ", ";
    private final Set<TicketGrantStrategy> grantStrategies;
    private final Set<TicketDenyStrategy> denyStrategies;
    private final UserInstance userInstance;
    private final TicketEntry ticket;

    public TicketPermissions(
        TicketEntry ticket,
        UserInstance userInstance,
        Resource resource,
        PublicationPermissions publicationPermissions) {
        this.userInstance = userInstance;
        this.ticket = ticket;
        this.grantStrategies = Set.of(
            new ApproveTicketGrantStrategy(ticket, userInstance, resource),
            new TransferTicketGrantStrategy(ticket, userInstance, resource),
            new ReadTicketGrantStrategy(ticket, userInstance, resource, publicationPermissions)
        );
        this.denyStrategies = Set.of(
            new ClaimedChannelTicketDenyStrategy(ticket, userInstance, resource),
            new FinalizedTicketDenyStrategy(ticket, userInstance, resource)
        );
    }

    public static TicketPermissions create(TicketEntry ticket, UserInstance userInstance, Resource resource, PublicationPermissions publicationPermissions) {
        return new TicketPermissions(ticket, userInstance, resource, publicationPermissions);
    }

    public boolean allowsAction(TicketOperation permission) {
        return !findAllowances(permission).isEmpty()
               && findDenials(permission).isEmpty();
    }

    public Set<TicketOperation> getAllAllowedActions() {
        return Arrays.stream(TicketOperation.values())
                   .filter(this::allowsAction)
                   .collect(Collectors.toSet());
    }

    public void authorize(TicketOperation requestedPermission) throws UnauthorizedException {
        validateDenyStrategiesRestrictions(requestedPermission);
        validateGrantStrategies(requestedPermission);
    }

    private List<TicketGrantStrategy> findAllowances(TicketOperation permission) {
        return grantStrategies.stream()
                   .filter(strategy -> strategy.allowsAction(permission))
                   .toList();
    }

    private List<TicketDenyStrategy> findDenials(TicketOperation permission) {
        return denyStrategies.stream()
                   .filter(strategy -> strategy.deniesAction(permission))
                   .toList();
    }

    private void validateDenyStrategiesRestrictions(TicketOperation requestedPermission)
        throws UnauthorizedException {
        var strategies = findDenials(requestedPermission).stream()
                                     .map(TicketDenyStrategy::getClass)
                                     .map(Class::getSimpleName)
                                     .toList();

        if (!strategies.isEmpty()) {
            logger.info("User {} was denied access {} on ticket {} from strategies {}",
                        userInstance.getUsername(),
                        requestedPermission,
                        ticket.getIdentifier(),
                        String.join(COMMA_DELIMITER, strategies));

            throw new UnauthorizedException(formatUnauthorizedMessage(requestedPermission));
        }
    }

    private void validateGrantStrategies(TicketOperation requestedPermission) throws UnauthorizedException {
        var strategies = findAllowances(requestedPermission).stream()
                                  .map(TicketGrantStrategy::getClass)
                                  .map(Class::getSimpleName)
                                  .toList();

        if (strategies.isEmpty()) {
            throw new UnauthorizedException(formatUnauthorizedMessage(requestedPermission));
        }

        logger.info("User {} was allowed {} on ticket {} from strategies {}",
                    userInstance.getUsername(),
                    requestedPermission,
                    ticket.getIdentifier(),
                    String.join(COMMA_DELIMITER, strategies));
    }

    private String formatUnauthorizedMessage(TicketOperation requestedPermission) {
        return String.format("Unauthorized: %s is not allowed to perform %s on %s", userInstance.getUsername(),
                             requestedPermission, ticket.getIdentifier());
    }

}

