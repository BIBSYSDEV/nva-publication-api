package no.unit.nva.publication.ticket.create;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.PublicationServiceConfig.PUBLICATION_PATH;
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
import java.util.Optional;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.TicketDto;
import no.unit.nva.publication.ticket.model.identityservice.CustomerTransactionResult;
import nva.commons.apigateway.AccessRight;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ForbiddenException;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

public class CreateTicketHandler extends ApiGatewayHandler<TicketDto, Void> {

    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String LOCATION_HEADER = "Location";
    public static final String PUBLICATION_IDENTIFIER = "publicationIdentifier";
    private final Logger logger = LoggerFactory.getLogger(CreateTicketHandler.class);
    private final HttpClient httpClient;
    private final SecretsReader secretsReader;
    private final TicketService ticketService;
    private final ResourceService resourceService;

    @JacocoGenerated
    public CreateTicketHandler() {
        this(TicketService.defaultService(), ResourceService.defaultService(),
             HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build(),
             SecretsReader.defaultSecretsManagerClient());
    }

    public CreateTicketHandler(TicketService ticketService, ResourceService resourceService,
                               HttpClient httpClient, SecretsManagerClient secretsManagerClient) {
        super(TicketDto.class);
        this.ticketService = ticketService;
        this.resourceService = resourceService;
        this.httpClient = httpClient;
        this.secretsReader = new SecretsReader(secretsManagerClient);
    }

    protected static CognitoCredentials fetchCredentials(SecretsReader secretsReader) {
        var credentials = secretsReader.fetchClassSecret(BACKEND_CLIENT_SECRET_NAME, BackendClientCredentials.class);
        var uri = UriWrapper.fromHost(BACKEND_CLIENT_AUTH_URL).getUri();

        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }

    @Override
    protected Void processInput(TicketDto input, RequestInfo requestInfo, Context context)
        throws ApiGatewayException {
        var publicationIdentifier = new SortableIdentifier(requestInfo.getPathParameter(PUBLICATION_IDENTIFIER));
        var publication = fetchPublication(publicationIdentifier, getUser(requestInfo), requestInfo);
        var ticketType = input.ticketType();
        var ticket = persistTicket(TicketEntry.requestNewTicket(publication, ticketType));
        var customer = requestInfo.getCurrentCustomer();

        if (registratorPublishesMetadataAndFiles(ticketType, customer)) {
            updateStatusToApproved(ticket);
            publishPublication(publication);
        } else {
            var ticketLocation = createTicketLocation(publicationIdentifier, ticket);
            addAdditionalHeaders(() -> Map.of(LOCATION_HEADER, ticketLocation));
        }

        return null;
    }

    private static UserInstance getUser(RequestInfo requestInfo) throws UnauthorizedException {
        return UserInstance.fromRequestInfo(requestInfo);
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

    private static URI getPublisherId(Publication publication) {
        return Optional.ofNullable(publication.getPublisher()).map(Organization::getId).orElse(null);
    }

    private static boolean hasValidAccessRights(RequestInfo requestInfo) {
        return requestInfo.userIsAuthorized(AccessRight.APPROVE_DOI_REQUEST.toString())
               || requestInfo.userIsAuthorized(AccessRight.REJECT_DOI_REQUEST.toString());
    }

    private boolean registratorPublishesMetadataAndFiles(Class<? extends TicketEntry> ticketType, URI customer) {
        return isPublishingRequest(ticketType) && customerAllowsPublishing(customer);
    }

    private void publishPublication(Publication publication) {
        attempt(() -> resourceService.publishPublication(UserInstance.fromPublication(publication),
                                                         publication.getIdentifier()));
    }

    private void updateStatusToApproved(TicketEntry createdTicket) {
        attempt(() -> ticketService.updateTicketStatus(createdTicket, TicketStatus.COMPLETED))
            .orElseThrow();
    }

    private boolean isPublishingRequest(Class<? extends TicketEntry> ticketType) {
        return PublishingRequestCase.class.equals(ticketType);
    }

    private boolean userIsAuthorized(RequestInfo requestInfo, Publication publication) throws UnauthorizedException {
        return hasValidAccessRights(requestInfo) && matchingCustomer(requestInfo, publication);
    }

    private boolean matchingCustomer(RequestInfo requestInfo, Publication publication)
        throws UnauthorizedException {
        return Optional.ofNullable(requestInfo.getCurrentCustomer())
                   .map(customer -> customer.equals(getPublisherId(publication)))
                   .orElse(false);
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

    private Publication fetchPublication(SortableIdentifier publicationIdentifier, UserInstance user,
                                         RequestInfo requestInfo)
        throws ApiGatewayException {
        return attempt(() -> resourceService.getPublication(user, publicationIdentifier))
                   .or(() -> fetchPublicationForPrivilegedUser(publicationIdentifier, requestInfo))
                   .orElseThrow(fail -> loggingFailureReporter(fail.getException()));
    }

    private Publication fetchPublicationForPrivilegedUser(SortableIdentifier publicationIdentifier,
                                                          RequestInfo requestInfo) throws ApiGatewayException {
        var publication = resourceService.getPublicationByIdentifier(publicationIdentifier);
        if (!userIsAuthorized(requestInfo, publication)) {
            throw new ForbiddenException();
        }
        return publication;
    }

    private ApiGatewayException loggingFailureReporter(Exception exception) {
        logger.error("Request failed: {}", Arrays.toString(exception.getStackTrace()));
        return new ForbiddenException();
    }

    private boolean customerAllowsPublishing(URI customerId) {
        var credentials = fetchCredentials(secretsReader);
        var backendClient = AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient, credentials);
        var fetchCustomerResult = attempt(() -> fetchCustomer(backendClient, customerId)).orElseThrow();
        return fetchCustomerResult.isKnownThatCustomerAllowsPublishing();
    }

    private CustomerTransactionResult fetchCustomer(AuthorizedBackendClient backendClient, URI customerId)
        throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(customerId).GET();
        var response = backendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new CustomerTransactionResult(response, customerId);
    }
}
