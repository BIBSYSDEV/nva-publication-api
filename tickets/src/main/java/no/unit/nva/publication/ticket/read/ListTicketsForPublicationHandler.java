package no.unit.nva.publication.ticket.read;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permission.strategy.PublicationPermissionStrategy;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketHandler;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;

public class ListTicketsForPublicationHandler extends TicketHandler<Void, TicketCollection> {

    private final ResourceService resourceService;

    private final TicketService ticketService;
    private final UriRetriever uriRetriever;

    @JacocoGenerated
    public ListTicketsForPublicationHandler() {
        this(ResourceService.defaultService(), TicketService.defaultService(), UriRetriever.defaultUriRetriever());
    }

    public ListTicketsForPublicationHandler(ResourceService resourceService, TicketService ticketService,
                                            UriRetriever uriRetriever) {
        super(Void.class);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.uriRetriever = uriRetriever;
    }

    @Override
    protected TicketCollection processInput(Void input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = extractPublicationIdentifierFromPath(requestInfo);
        var userInstance = UserInstance.fromRequestInfo(requestInfo);
        var requestUtils = RequestUtils.fromRequestInfo(requestInfo, uriRetriever);
        var ticketDtos = fetchTickets(requestUtils, publicationIdentifier, userInstance);
        return TicketCollection.fromTickets(ticketDtos);
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, TicketCollection output) {
        return HttpURLConnection.HTTP_OK;
    }

    private List<TicketDto> fetchTickets(RequestUtils requestUtils, SortableIdentifier publicationIdentifier,
                                         UserInstance userInstance) throws ApiGatewayException {
        var tickets = fetchTickets(userInstance, publicationIdentifier)
                          .filter(ticket -> isOwner(userInstance, ticket) || requestUtils.isAuthorizedToManage(ticket));

        return tickets.map(this::createDto).toList();
    }

    private static Boolean isOwner(UserInstance userInstance, TicketEntry ticket) {
        return UserInstance.fromTicket(ticket).getUsername().equals(userInstance.getUsername());
    }

    private Stream<TicketEntry> fetchTickets(UserInstance userInstance, SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        return Optional.ofNullable(resourceService.getResourceByIdentifier(publicationIdentifier))
                   .filter(resource -> isAllowedToListTickets(userInstance, resource))
                   .map(resourceService::fetchAllTicketsForResource)
                   .orElseThrow(ForbiddenException::new);
    }

    private boolean isAllowedToListTickets(UserInstance userInstance, Resource resource) {
        return PublicationPermissionStrategy.create(resource.toPublication(), userInstance, uriRetriever)
                   .allowsAction(PublicationOperation.UPDATE);
    }

    private TicketDto createDto(TicketEntry ticket) {
        var messages = ticket.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticket, messages);
    }
}
