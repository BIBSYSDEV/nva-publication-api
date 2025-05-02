package no.unit.nva.publication.ticket.create;

import static no.unit.nva.model.PublicationOperation.DOI_REQUEST_CREATE;
import static no.unit.nva.model.PublicationOperation.SUPPORT_REQUEST_CREATE;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.DoiRequestDto;
import no.unit.nva.publication.ticket.GeneralSupportRequestDto;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketResolver {

    public static final String CREATING_TICKET_ERROR_MESSAGE =
        "Creating ticket {} for publication {} is forbidden for user {}";
    private final Logger logger = LoggerFactory.getLogger(TicketResolver.class);
    private final ResourceService resourceService;
    private final TicketService ticketService;

    @JacocoGenerated
    public TicketResolver() {
        this(ResourceService.defaultService(), TicketService.defaultService());
    }

    public TicketResolver(ResourceService resourceService, TicketService ticketService) {
        this.resourceService = resourceService;
        this.ticketService = ticketService;
    }

    public TicketEntry resolveAndPersistTicket(TicketDto ticketDto, RequestUtils requestUtils)
        throws ApiGatewayException {
        var resource = fetchResource(requestUtils);
        var userInstance = requestUtils.toUserInstance();

        validateUserPermissions(resource, ticketDto, userInstance);

        var ticket = switch (ticketDto) {
            case DoiRequestDto ignore -> DoiRequest.create(resource, userInstance);
            case GeneralSupportRequestDto ignore -> GeneralSupportRequest.create(resource, userInstance);
            default -> throw new BadRequestException("Not supported ticket type");
        };
        return ticket.persistNewTicket(ticketService);
    }

    private static boolean userHasPermissionToCreateTicket(PublicationPermissions permissionStrategy,
                                                           TicketDto ticketDto) {
        var allowedActions = permissionStrategy.getAllAllowedActions();
        return switch (ticketDto) {
            case DoiRequestDto ignored -> allowedActions.contains(DOI_REQUEST_CREATE);
            case GeneralSupportRequestDto ignored -> allowedActions.contains(SUPPORT_REQUEST_CREATE);
            case null, default -> false;
        };
    }

    private void validateUserPermissions(Resource resource, TicketDto ticketDto, UserInstance userInstance)
        throws ForbiddenException {
        var permissionStrategy = PublicationPermissions.create(resource.toPublication(), userInstance);
        if (!userHasPermissionToCreateTicket(permissionStrategy, ticketDto)) {
            logger.error(CREATING_TICKET_ERROR_MESSAGE,
                         ticketDto.ticketType().getSimpleName(),
                         resource.getIdentifier(),
                         userInstance.getUser().toString());
            throw new ForbiddenException();
        }
    }

    private Resource fetchResource(RequestUtils requestUtils) throws ApiGatewayException {
        var resourceIdentifier = requestUtils.publicationIdentifier();
        return Resource.resourceQueryObject(resourceIdentifier)
                   .fetch(resourceService)
                   .orElseThrow(() -> new NotFoundException("Publication not found"));
    }
}
