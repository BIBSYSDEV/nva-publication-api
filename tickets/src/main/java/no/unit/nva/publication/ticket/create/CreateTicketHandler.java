package no.unit.nva.publication.ticket.create;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.external.services.AuthorizedBackendUriRetriever;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class CreateTicketHandler extends ApiGatewayHandler<TicketDto, Void> {

    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String LOCATION_HEADER = "Location";
    private final TicketResolver ticketResolver;
    private final MessageService messageService;

    @JacocoGenerated
    public CreateTicketHandler() {
        this(new TicketResolver(ResourceService.defaultService(), TicketService.defaultService(),
                                new AuthorizedBackendUriRetriever(BACKEND_CLIENT_AUTH_URL,
                                                                  BACKEND_CLIENT_SECRET_NAME)),
             MessageService.defaultService(),
             new Environment());
    }

    public CreateTicketHandler(TicketResolver ticketResolver, MessageService messageService, Environment environment) {
        super(TicketDto.class, environment);
        this.ticketResolver = ticketResolver;
        this.messageService = messageService;
    }

    @Override
    protected void validateRequest(TicketDto ticketDto, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        //Do nothing
    }

    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        try {
            var requestUtils = RequestUtils.fromRequestInfo(requestInfo);
            var persistedTicket = ticketResolver.resolveAndPersistTicket(input, requestUtils);
            persistMessage(input, requestUtils, persistedTicket);
            addLocationHeader(requestUtils.publicationIdentifier(), persistedTicket.getIdentifier());
            return null;
        } catch (IllegalStateException exception) {
            throw new ConflictException(exception, exception.getMessage());
        }
    }

    private void persistMessage(TicketDto input, RequestUtils requestUtils, TicketEntry ticket) {
        if (!input.getMessages().isEmpty()) {
            var message = input.getMessages().getFirst().getText();
            messageService.createMessage(ticket, requestUtils.toUserInstance(), message);
        }
    }

    private void addLocationHeader(SortableIdentifier publicationIdentifier, SortableIdentifier ticketIdentifier) {
        var ticketLocation = createTicketLocation(publicationIdentifier, ticketIdentifier);
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, ticketLocation));
    }

    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }

    private static String createTicketLocation(SortableIdentifier publicationIdentifier,
                                               SortableIdentifier ticketIdentifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_PATH)
                   .addChild(publicationIdentifier.toString())
                   .addChild(PublicationServiceConfig.TICKET_PATH)
                   .addChild(ticketIdentifier.toString())
                   .getUri()
                   .toString();
    }
}
