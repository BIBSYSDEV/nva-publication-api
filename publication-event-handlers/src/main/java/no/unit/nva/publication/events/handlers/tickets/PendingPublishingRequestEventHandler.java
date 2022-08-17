package no.unit.nva.publication.events.handlers.tickets;

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
import java.time.Clock;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.handlers.DestinationsEventBridgeEventHandler;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.events.handlers.tickets.identityservice.CustomerDto;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

public class PendingPublishingRequestEventHandler
    extends DestinationsEventBridgeEventHandler<EventReference, Void> {
    
    private static final Logger logger = LoggerFactory.getLogger(PendingPublishingRequestEventHandler.class);
    private final S3Driver s3Driver;
    private final PublishingRequestService publishingRequestService;
    private final HttpClient httpClient;
    
    @JacocoGenerated
    public PendingPublishingRequestEventHandler() {
        this(defaultPublishingRequestService(), HttpClient.newHttpClient(), DEFAULT_S3_CLIENT);
    }
    
    protected PendingPublishingRequestEventHandler(PublishingRequestService publishingRequestService,
                                                   HttpClient httpClient,
                                                   S3Client s3Client) {
        super(EventReference.class);
        this.s3Driver = new S3Driver(s3Client, PublicationEventsConfig.EVENTS_BUCKET);
        this.publishingRequestService = publishingRequestService;
        this.httpClient = httpClient;
    }
    
    @Override
    protected Void processInputPayload(EventReference input,
                                       AwsEventBridgeEvent<AwsEventBridgeDetail<EventReference>> event,
                                       Context context) {
        var updateEvent = parseInput(input);
        var publishingRequest = extractPublishingRequestCaseUpdate(updateEvent);
        if (customerAllowsPublishing(publishingRequest)) {
            attempt(() -> publishingRequestService.completeTicket(publishingRequest)).orElseThrow();
        }
        
        return null;
    }
    
    @JacocoGenerated
    private static PublishingRequestService defaultPublishingRequestService() {
        return
            new PublishingRequestService(PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT, Clock.systemDefaultZone());
    }
    
    private boolean customerAllowsPublishing(PublishingRequestCase publishingRequest) {
        var customerId = publishingRequest.getCustomerId();
        var fetchCustomerResult = attempt(() -> fetchCustomer(customerId)).orElseThrow();
        return fetchCustomerResult.isKnownThatCustomerAllowsPublishing();
    }
    
    private HttpTransactionResult fetchCustomer(URI customerId) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(customerId).GET().build();
        var response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
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
        
        public boolean isKnownThatCustomerAllowsPublishing() {
            return customer.map(CustomerDto::customerAllowsRegistratorsToPublishDataAndMetadata)
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
