package no.unit.nva.publication.ticket.update;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.doi.requirements.DoiResourceRequirements;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.TicketHandler;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;

public class UpdateTicketStatusHandler extends TicketHandler<TicketDto, Void> {

    public static final String PUBLICATION_WITH_IDENTIFIER_S_DOES_NOT_SATISFY_DOI_REQUIREMENTS
        = "Publication with identifier  %s, does not satisfy DOI requirements";
    private final TicketService ticketService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public UpdateTicketStatusHandler() {
        this(TicketService.defaultService(),
             ResourceService.defaultService());
    }

    public UpdateTicketStatusHandler(TicketService ticketService, ResourceService resourceService) {
        super(TicketDto.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
    }

    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context) throws ApiGatewayException {

        var ticketIdentifier = extractTicketIdentifierFromPath(requestInfo);
        var ticket = ticketService.fetchTicketByIdentifier(ticketIdentifier);

        if (userIsNotAuthorized(requestInfo, ticket)) {
            throw new ForbiddenException();
        }
        if (ticket instanceof DoiRequest) {
            doiTicketSideEffects(input, requestInfo);
        }
        ticketService.updateTicketStatus(ticket, input.getStatus());
        return null;
    }

    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_ACCEPTED;
    }

    private static boolean userIsNotAuthorized(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return !(isAuthorizedToCompleteTickets(requestInfo) && isUserFromSameCustomerAsTicket(requestInfo, ticket));
    }

    private static boolean isUserFromSameCustomerAsTicket(RequestInfo requestInfo, TicketEntry ticket)
        throws UnauthorizedException {
        return requestInfo.getCurrentCustomer().equals(ticket.getCustomerId());
    }

    private static boolean isAuthorizedToCompleteTickets(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString());
    }

    private void doiTicketSideEffects(TicketDto input, final RequestInfo requestInfo)
        throws NotFoundException, BadMethodException {
        if (TicketStatus.COMPLETED.equals(input.getStatus())) {
            var publication = getPublication(requestInfo);
            publicationSatisfiesDoiRequirements(publication);
        }
    }

    private Publication getPublication(RequestInfo requestInfo) throws NotFoundException {
        var publicationIdentifier = extractPublicationIdentifierFromPath(requestInfo);
        return resourceService.getPublicationByIdentifier(publicationIdentifier);
    }

    private void publicationSatisfiesDoiRequirements(Publication publication)
        throws BadMethodException {
        if (!DoiResourceRequirements.publicationSatisfiesDoiRequirements(publication)) {
            throw new BadMethodException(
                String.format(PUBLICATION_WITH_IDENTIFIER_S_DOES_NOT_SATISFY_DOI_REQUIREMENTS,
                              publication.getIdentifier()));
        }
    }
}
