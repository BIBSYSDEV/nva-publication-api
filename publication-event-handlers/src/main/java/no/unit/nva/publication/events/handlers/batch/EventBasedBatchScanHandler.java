package no.unit.nva.publication.events.handlers.batch;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import java.time.Clock;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.bodies.ScanDatabaseRequest;
import no.unit.nva.publication.events.handlers.PublicationEventsConfig;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DataEntry;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

public class EventBasedBatchScanHandler extends EventHandler<ScanDatabaseRequest, Void> {

    public static final String DETAIL_TYPE = "NO_DETAIL_TYPE";
    public static final String EVENT_BUS_NAME =
        PublicationEventsConfig.ENVIRONMENT.readEnv("EVENT_BUS_NAME");
    private final ResourceService resourceService;
    private final EventBridgeClient eventBridgeClient;
    private final Logger logger = LoggerFactory.getLogger(EventBasedBatchScanHandler.class);

    @JacocoGenerated
    public EventBasedBatchScanHandler() {
        this(defaultResourceService(), defaultEventBridgeClient());
    }

    public EventBasedBatchScanHandler(ResourceService resourceService, EventBridgeClient eventBridgeClient) {
        super(ScanDatabaseRequest.class);
        this.resourceService = resourceService;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    protected Void processInput(ScanDatabaseRequest input, AwsEventBridgeEvent<ScanDatabaseRequest> event,
                                Context context) {
        ListingResult<DataEntry> result =
            resourceService.scanResources(input.getPageSize(), input.getStartMarker());
        resourceService.refreshResources(result.getDatabaseEntries());
        logger.info("Query starting point:" + input.getStartMarker());
        if (result.isTruncated()) {
            sendEventToInvokeNewRefreshRowVersionExecution(input, context, result);
        }
        return null;
    }

    @JacocoGenerated
    private static EventBridgeClient defaultEventBridgeClient() {
        return EventBridgeClient.builder()
            .httpClientBuilder(UrlConnectionHttpClient.builder())
            .build();
    }

    @JacocoGenerated
    private static ResourceService defaultResourceService() {
        return new ResourceService(defaultDynamoDbClient(), Clock.systemDefaultZone());
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoDbClient() {
        return AmazonDynamoDBClientBuilder
            .standard()
            .withRegion(PublicationEventsConfig.AWS_REGION)
            .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
            .build();
    }

    private void sendEventToInvokeNewRefreshRowVersionExecution(ScanDatabaseRequest input,
                                                                Context context,
                                                                ListingResult<DataEntry> result) {
        PutEventsRequestEntry newEvent = input
            .newScanDatabaseRequest(result.getStartMarker())
            .createNewEventEntry(EVENT_BUS_NAME, DETAIL_TYPE, context.getInvokedFunctionArn());
        sendEvent(newEvent);
    }

    private void sendEvent(PutEventsRequestEntry putEventRequestEntry) {
        PutEventsRequest putEventRequest = PutEventsRequest.builder()
            .entries(putEventRequestEntry)
            .build();
        eventBridgeClient.putEvents(putEventRequest);
    }
}
