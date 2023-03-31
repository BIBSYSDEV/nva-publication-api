package no.unit.nva.publication.ticket.create;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
import static no.unit.nva.publication.events.handlers.tickets.PendingPublishingRequestEventHandler.fetchCredentials;
import static no.unit.nva.publication.events.handlers.tickets.PendingPublishingRequestEventHandler.getCognitoTokenUrl;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.publication.events.handlers.tickets.PendingPublishingRequestEventHandler.HttpTransactionResult;
import no.unit.nva.publication.events.handlers.tickets.identityservice.CustomerDto;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateTicketHandler extends ApiGatewayHandler<TicketDto, Void> {

    private final Logger logger = LoggerFactory.getLogger(CreateTicketHandler.class);
    public static final String LOCATION_HEADER = "Location";
    private final TicketService ticketService;
    private final ResourceService resourceService;
    private final HttpClient httpClient;
    private final SecretsReader secretsReader;
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");


    @JacocoGenerated
    public CreateTicketHandler() {
        this(TicketService.defaultService(), ResourceService.defaultService(), httpClient, secretsReader);
    }
    
    public CreateTicketHandler(TicketService ticketService, ResourceService resourceService,
                               HttpClient httpClient, SecretsReader secretsReader) {
        super(TicketDto.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.httpClient = httpClient;
        this.secretsReader = secretsReader;
    }
    
    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = new SortableIdentifier(requestInfo.getPathParameter("publicationIdentifier"));
        var user = UserInstance.fromRequestInfo(requestInfo);
        var publication = fetchPublication(publicationIdentifier, user);
        var newTicket = TicketEntry.requestNewTicket(publication, input.ticketType());
        var createdTicket = persistTicket(newTicket);
        var ticketLocation = createTicketLocation(publicationIdentifier, createdTicket);
        addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, ticketLocation));
    
        return null;
    }
    
    @Override
    protected Integer getSuccessStatusCode(TicketDto input, Void output) {
        return HttpURLConnection.HTTP_CREATED;
    }
    
    private static String createTicketLocation(SortableIdentifier publicationIdentifier, TicketEntry createdTicket) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PUBLICATION_PATH)
                   .addChild(publicationIdentifier.toString())
                   .addChild(PublicationServiceConfig.TICKET_PATH)
                   .addChild(createdTicket.getIdentifier().toString())
                   .getUri()
                   .toString();
    }
    
    private TicketEntry persistTicket(TicketEntry newTicket) throws ApiGatewayException {
        return attempt(() -> newTicket.persistNewTicket(ticketService))
                   .orElse(fail -> handleCreationException(fail.getException(), newTicket));
    }
    
    private TicketEntry handleCreationException(Exception exception, TicketEntry newTicket) throws ApiGatewayException {
        if (exception instanceof TransactionFailedException) {
            return updateAlreadyExistingTicket(newTicket);
        }
        if (exception instanceof RuntimeException) {
            throw (RuntimeException) exception;
        }
        if (exception instanceof ApiGatewayException) {
            throw (ApiGatewayException) exception;
        }
        throw new RuntimeException(exception);
    }
    
    private TicketEntry updateAlreadyExistingTicket(TicketEntry newTicket) {
        var customerId = newTicket.getCustomerId();
        var resourceIdentifier = newTicket.extractPublicationIdentifier();
        return ticketService.fetchTicketByResourceIdentifier(customerId, resourceIdentifier, newTicket.getClass())
                   .map(this::updateTicket)
                   .orElseThrow();
    }
    
    private TicketEntry updateTicket(TicketEntry ticket) {
        ticket.persistUpdate(ticketService);
        return ticket;
    }
    
    private Publication fetchPublication(SortableIdentifier publicationIdentifier, UserInstance user)
        throws ApiGatewayException {
        return attempt(() -> resourceService.getPublication(user, publicationIdentifier))
                   .orElseThrow(fail -> loggingFailureReporter(fail.getException()));
    }

    private ApiGatewayException loggingFailureReporter(Exception exception) {
        logger.error("Request failed: {}", Arrays.toString(exception.getStackTrace()));
        return new ForbiddenException();
    }

    private CognitoCredentials fetchCredentials(SecretsReader secretsReader) {
        var credentials
            = secretsReader.fetchClassSecret(BACKEND_CLIENT_SECRET_NAME, BackendClientCredentials.class);
        var cognitoTokenUrl = UriWrapper.fromHost(BACKEND_CLIENT_AUTH_URL).getUri();

        return new CognitoCredentials(credentials::getId, credentials::getSecret, cognitoTokenUrl);
    }
    private CustomerDto fetchCustomer(URI customerId) throws IOException, InterruptedException {
        var credentials = fetchCredentials(this.secretsReader);
        var backendClient =
            AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient, credentials);

        var request = HttpRequest.newBuilder(customerId).GET();
        var response = backendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        return dtoObjectMapper.readValue(response.body(), CustomerDto.class);
    }
}
