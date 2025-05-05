package no.unit.nva.publication.ticket.read;

import static java.net.HttpURLConnection.HTTP_OK;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.GoneException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class GetTicketHandler extends ApiGatewayHandler<Void, TicketDto> {
    
    private static final String TICKET_NOT_FOUND = "Ticket not found";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    
    @JacocoGenerated
    public GetTicketHandler() {
        this(TicketService.defaultService(), ResourceService.defaultService(), new Environment());
    }
    
    public GetTicketHandler(TicketService ticketService, ResourceService resourceService, Environment environment) {
        super(Void.class, environment);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
    }

    @Override
    protected void validateRequest(Void unused, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        //Do nothing
    }

    private static void validateRequest(SortableIdentifier publicationIdentifier, TicketEntry ticket)
        throws NotFoundException, GoneException {
        if (!ticket.getResourceIdentifier().equals(publicationIdentifier)) {
            throw new NotFoundException(TICKET_NOT_FOUND);
        }
        if (TicketStatus.REMOVED.equals(ticket.getStatus())) {
            throw new GoneException("Ticket has beem removed!");
        }
    }

    @Override
    protected TicketDto processInput(Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
        var requestUtils = RequestUtils.fromRequestInfo(requestInfo);
        var ticket = ticketService.fetchTicketByIdentifier(requestUtils.ticketIdentifier());
        var resource = resourceService.getResourceByIdentifier(ticket.getResourceIdentifier());
        validateRequest(requestUtils.publicationIdentifier(), ticket);
        var messages = ticket.fetchMessages(ticketService);
        return TicketDto.fromTicket(ticket, messages, getCuratingInstitutionsIdList(resource));
    }

    private static List<URI> getCuratingInstitutionsIdList(Resource resource) {
        return resource.getCuratingInstitutions().stream()
                   .map(CuratingInstitution::id)
                   .toList();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, TicketDto output) {
        return HTTP_OK;
    }
}
