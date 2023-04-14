package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.publication.PublicationServiceConfig.ENVIRONMENT;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.DEFAULT_S3_CLIENT;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import no.unit.nva.auth.AuthorizedBackendClient;
import no.unit.nva.auth.CognitoCredentials;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.events.handlers.tickets.identityservice.CustomerDto;
import no.unit.nva.publication.model.BackendClientCredentials;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.exceptions.ExceptionUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class PendingPublishingRequestEventHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(PendingPublishingRequestEventHandler.class);
    private final S3Driver s3Driver;
    private final HttpClient httpClient;
    private final SecretsReader secretsReader;
    public static final String BACKEND_CLIENT_AUTH_URL = ENVIRONMENT.readEnv("BACKEND_CLIENT_AUTH_URL");
    public static final String BACKEND_CLIENT_SECRET_NAME = ENVIRONMENT.readEnv("BACKEND_CLIENT_SECRET_NAME");
    private final ResourceService resourceService;
    
    @JacocoGenerated
    public PendingPublishingRequestEventHandler() {
        this(ResourceService.defaultService(),
             HttpClient.newHttpClient(),
             new SecretsReader(),
             DEFAULT_S3_CLIENT);
    }
    
    protected PendingPublishingRequestEventHandler(ResourceService resourceService,
                                                   HttpClient httpClient,
                                                   SecretsReader secretsReader,
                                                   S3Client s3Client) {
        super(EventReference.class);
        this.resourceService = resourceService;
        this.httpClient = httpClient;
        this.secretsReader = secretsReader;
        this.s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
    }
    
    @Override
    protected Void processInputPayload(EventReference input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                       Context context) {
        var updateEvent = parseInput(input);
        var publishingRequest = extractPublishingRequestCaseUpdate(updateEvent);

        var credentials = fetchCredentials(this.secretsReader);
        var backendClient = AuthorizedBackendClient.prepareWithCognitoCredentials(httpClient, credentials);

        if (customerAllowsMetadataPublishing(backendClient, publishingRequest)
            && ticketHasNotBeenCompleted(publishingRequest)) {
                publishMetadata(publishingRequest);
        }

        return null;
    }

    protected static CognitoCredentials fetchCredentials(SecretsReader secretsReader) {
        var credentials
            = secretsReader.fetchClassSecret(BACKEND_CLIENT_SECRET_NAME, BackendClientCredentials.class);
        var uri = getCognitoTokenUrl();

        return new CognitoCredentials(credentials::getId, credentials::getSecret, uri);
    }

    private static URI getCognitoTokenUrl() {
        return UriWrapper.fromHost(BACKEND_CLIENT_AUTH_URL).getUri();
    }

    private void publishMetadata(PublishingRequestCase publishingRequest) {
        var userInstance = UserInstance.create(publishingRequest.getOwner(), publishingRequest.getCustomerId());
        attempt(() -> resourceService.publishPublicationMetadata(userInstance,
                                                                 publishingRequest.extractPublicationIdentifier()))
            .orElse(fail -> logError(fail.getException()));
    }

    private PublishPublicationStatusResponse logError(Exception exception) {
        logger.warn(ExceptionUtils.stackTraceInSingleLine(exception));
        return null;
    }

    private static boolean ticketHasNotBeenCompleted(PublishingRequestCase publishingRequest) {
        return !TicketStatus.COMPLETED.equals(publishingRequest.getStatus());
    }
    
    @JacocoGenerated
    private static TicketService defaultPublishingRequestService() {
        return
            new TicketService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT);
    }

    private boolean customerAllowsMetadataPublishing(AuthorizedBackendClient backendClient,
                                                     PublishingRequestCase publishingRequest) {
        var customerId = publishingRequest.getCustomerId();
        var fetchCustomerResult = attempt(() -> fetchCustomer(backendClient, customerId)).orElseThrow();
        return fetchCustomerResult.isKnownThatCustomerAllowsPublishingOfMetadata();
    }
    
    private HttpTransactionResult fetchCustomer(AuthorizedBackendClient backendClient, URI customerId)
                                                                            throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(customerId).GET();
        var response = backendClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new HttpTransactionResult(response, customerId);
    }
    
    private PublishingRequestCase extractPublishingRequestCaseUpdate(DataEntryUpdateEvent updateEvent) {
        return Optional.ofNullable(updateEvent)
                   .map(DataEntryUpdateEvent::getNewData)
                   .map(PublishingRequestCase.class::cast)
                   .orElseThrow();
    }
    
    private DataEntryUpdateEvent parseInput(EventReference input) {
        var blob = s3Driver.readEvent(input.getUri());
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(blob, DataEntryUpdateEvent.class)).orElseThrow();
    }
    
    private static class HttpTransactionResult {
        
        private final HttpResponse<String> httpResponse;
        private final Try<CustomerDto> customer;
        private final URI customerId;
        
        public HttpTransactionResult(HttpResponse<String> response, URI customerId) {
            this.httpResponse = response;
            this.customer = attempt(() -> JsonUtils.dtoObjectMapper.readValue(response.body(), CustomerDto.class));
            this.customerId = customerId;
        }

        public boolean isKnownThatCustomerAllowsPublishingOfMetadata() {
            return customer.map(CustomerDto::customerAllowsRegistratorsToPublishMetadata)
                       .orElse(fail -> returnFalseAndLogUnsuccessfulResponse(httpResponse, customerId));
        }
        
        private boolean returnFalseAndLogUnsuccessfulResponse(HttpResponse<String> response, URI customerId) {
            logger.warn(new FailedResponseMessage(response, customerId).toString());
            return false;
        }
    }
    
    private static class FailedResponseMessage {
        
        private final HttpResponse<String> response;
        private final URI customerId;
        
        public FailedResponseMessage(HttpResponse<String> response, URI customerId) {
            this.response = response;
            this.customerId = customerId;
        }
        
        @JsonProperty("statusCode")
        public int getStatusCode() {
            return response.statusCode();
        }
        
        @JsonProperty("body")
        public String body() {
            return response.body();
        }
        
        @JsonProperty
        public URI getCustomerId() {
            return customerId;
        }
        
        @Override
        public String toString() {
            return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(this)).orElseThrow();
        }
    }
}
