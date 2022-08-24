package no.unit.nva.publication.publishingrequest.create;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.publishingrequest.TicketDto;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.paths.UriWrapper;

public class CreateTicketHandler extends ApiGatewayHandler<TicketDto, Void> {
    
    public static final String LOCATION_HEADER = "Location";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    
    public CreateTicketHandler(TicketService ticketService, ResourceService resourceService) {
        super(TicketDto.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
    }
    
    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = new SortableIdentifier(requestInfo.getPathParameter("publicationIdentifier"));
        var user = UserInstance.fromRequestInfo(requestInfo);
        var publication = fetchPublication(publicationIdentifier, user);
        var newTicket = TicketEntry.requestNewTicket(publication, input.ticketType());
        var createdTicket = ticketService.createTicket(newTicket, newTicket.getClass());
        var ticketLocation = createTicketLocation(publicationIdentifier, createdTicket);
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, ticketLocation));
    
        return null;
    }
    
    private Publication fetchPublication(SortableIdentifier publicationIdentifier, UserInstance user)
        throws ForbiddenException {
        return attempt(() -> resourceService.getPublication(user, publicationIdentifier))
                   .orElseThrow(fail -> new ForbiddenException());
    }
    
    private static String createTicketLocation(SortableIdentifier publicationIdentifier, TicketEntry createdTicket) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild("publication")
                   .addChild(publicationIdentifier.toString())
                   .addChild("ticket")
                   .addChild(createdTicket.getIdentifier().toString())
                   .getUri()
                   .toString();
    }
    
    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_SEE_OTHER;
    }
}
