package no.unit.nva.publication.ticket.read;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ListTicketsForPublicationHandler extends TicketHandler<Void, TicketCollection> {

    private final ResourceService resourceService;

    private final TicketService ticketService;

    @JacocoGenerated
    public ListTicketsForPublicationHandler() {
        this(ResourceService.defaultService(), TicketService.defaultService(), new Environment());
    }

    public ListTicketsForPublicationHandler(ResourceService resourceService, TicketService ticketService,
                                            Environment environment) {
        super(Void.class, environment);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected TicketCollection processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = extractPublicationIdentifierFromPath(requestInfo);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var ticketDtos = fetchTickets(publicationIdentifier, userInstance);
        return TicketCollection.fromTickets(ticketDtos);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, TicketCollection output) {
        return HttpURLConnection.HTTP_OK;
    }

    private List<TicketDto> fetchTickets(SortableIdentifier publicationIdentifier,
                                         UserInstance userInstance) throws ApiGatewayException {
        var resource = resourceService.getResourceByIdentifier(publicationIdentifier);
        var tickets = fetchTickets(userInstance, resource)
                          .filter(ticketEntry -> hasAccessToTicket(ticketEntry, userInstance));


        return tickets.map(ticketEntry -> createDto(ticketEntry, resource)).toList();
    }

    private boolean hasAccessToTicket(TicketEntry ticketEntry, UserInstance userInstance) {
        if (ticketEntry.getOwner().equals(userInstance.getUser())) {
            return true;
        }
        if (ticketEntry instanceof GeneralSupportRequest) {
            return ticketEntry.hasSameOwnerAffiliationAs(userInstance);
        } else {
            return ticketEntry.hasSameOwnerAffiliationAs(userInstance)
                   || !TicketStatus.PENDING.equals(ticketEntry.getStatus());
        }
    }

    private Stream<TicketEntry> fetchTickets(UserInstance userInstance, Resource resource)
        throws ApiGatewayException {
        return Optional.ofNullable(resource)
                   .filter(ticketResource -> isAllowedToListTickets(userInstance, ticketResource))
                   .map(resourceService::fetchAllTicketsForResource)
                   .orElseThrow(ForbiddenException::new);
    }

    private boolean isAllowedToListTickets(UserInstance userInstance, Resource resource) {
        return PublicationPermissions.create(resource, userInstance)
                   .allowsAction(PublicationOperation.PARTIAL_UPDATE);
    }

    private TicketDto createDto(TicketEntry ticket, Resource resource) {
        var messages = ticket.fetchMessages(ticketService);
        var curatingInstitutions = resource.getCuratingInstitutions().stream().map(CuratingInstitution::id).toList();
        return TicketDto.fromTicket(ticket, messages, curatingInstitutions);
    }
}
