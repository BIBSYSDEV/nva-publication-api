package no.unit.nva.publication.ticket.read;

import static nva.commons.apigateway.AccessRight.MANAGE_DOI;
import static nva.commons.apigateway.AccessRight.MANAGE_PUBLISHING_REQUESTS;
import static nva.commons.apigateway.AccessRight.SUPPORT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketHandler;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class ListTicketsForPublicationHandler extends TicketHandler<Void, TicketCollection> {

    private final ResourceService resourceService;

    private final TicketService ticketService;
    private final UriRetriever uriRetriever;

    @JacocoGenerated
    public ListTicketsForPublicationHandler() {
        this(ResourceService.defaultService(), TicketService.defaultService(), RequestUtils.defaultUriRetriever());
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
        var ticketEntries = isNotPublicationOwnerAndHasAccessRightsToListTickets(requestUtils, publicationIdentifier)
                                ? fetchTicketsForElevatedUser(userInstance, publicationIdentifier)
                                      .filter(ticket -> requestUtils.isAuthorizedToManage(ticket, resourceService))
                                : fetchTicketsForPublicationOwner(publicationIdentifier, userInstance);

        return ticketEntries.map(this::createDto).collect(Collectors.toList());
    }

    private boolean isNotPublicationOwnerAndHasAccessRightsToListTickets(RequestUtils requestUtils,
                                                                         SortableIdentifier publicationIdentifier)
        throws NotFoundException {
        return !isPublicationOwner(requestUtils, publicationIdentifier)
               && requestUtils.hasOneOfAccessRights(MANAGE_DOI, MANAGE_PUBLISHING_REQUESTS, SUPPORT);
    }

    private boolean isPublicationOwner(RequestUtils requestUtils, SortableIdentifier publicationIdentifier)
        throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        return requestUtils.username().equals(publication.getResourceOwner().getOwner().getValue());
    }

    private Stream<TicketEntry> fetchTicketsForPublicationOwner(SortableIdentifier publicationIdentifier,
                                                                UserInstance userInstance) throws ApiGatewayException {

        return attempt(() -> resourceService.fetchAllTicketsForPublication(userInstance, publicationIdentifier))
                   .orElseThrow(fail -> handleFetchingError(fail.getException()));
    }

    private Stream<TicketEntry> fetchTicketsForElevatedUser(UserInstance userInstance,
                                                            SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {

        return attempt(() -> resourceService.fetchAllTicketsForElevatedUser(userInstance, publicationIdentifier))
                   .orElseThrow(fail -> handleFetchingError(fail.getException()));
    }

    private TicketDto createDto(TicketEntry ticket) {
        var messages = ticket.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticket, messages);
    }

    private ApiGatewayException handleFetchingError(Exception exception) {
        return switch (exception) {
            case NotFoundException notFoundException -> new ForbiddenException();
            case ApiGatewayException apiGatewayException -> apiGatewayException;
            case RuntimeException runtimeException -> throw runtimeException;
            case null, default -> throw new RuntimeException(exception);
        };
    }
}
