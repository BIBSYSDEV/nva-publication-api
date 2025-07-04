package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.publication.events.handlers.ConfigurationForPushingDirectlyToEventBridge.EVENT_BUS_NAME;
import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.publication.events.bodies.ScanDatabaseRequest;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

public class EventBasedBatchScanHandler extends EventHandler<ScanDatabaseRequest, Void> {

    public static final String DETAIL_TYPE = "NO_DETAIL_TYPE";
    private static final String CRISTIN_UNITS_S3_URI_ENV = "CRISTIN_UNITS_S3_URI";
    private final ResourceService resourceService;
    private final EventBridgeClient eventBridgeClient;
    private final CristinUnitsUtil  cristinUnitsUtil;
    private final Logger logger = LoggerFactory.getLogger(EventBasedBatchScanHandler.class);

    @JacocoGenerated
    public EventBasedBatchScanHandler() {
        this(ResourceService.defaultService(), defaultEventBridgeClient(),
             new CristinUnitsUtil(S3Client.create(), new Environment().readEnv(CRISTIN_UNITS_S3_URI_ENV)));
    }

    public EventBasedBatchScanHandler(ResourceService resourceService,
                                      EventBridgeClient eventBridgeClient,
                                      CristinUnitsUtil cristinUnitsUtil) {
        super(ScanDatabaseRequest.class);
        this.resourceService = resourceService;
        this.eventBridgeClient = eventBridgeClient;
        this.cristinUnitsUtil = cristinUnitsUtil;
    }

    @Override
    protected Void processInput(ScanDatabaseRequest input, AwsEventBridgeEvent<ScanDatabaseRequest> event,
                                Context context) {
        logger.info("Query starting point: {}", input.getStartMarker());
        var result = resourceService.scanResources(input.getPageSize(), input.getStartMarker(), input.getTypes());
        resourceService.refreshResources(result.getDatabaseEntries(), cristinUnitsUtil);
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

    private void sendEventToInvokeNewRefreshRowVersionExecution(ScanDatabaseRequest input,
                                                                Context context,
                                                                ListingResult<Entity> result) {
        PutEventsRequestEntry newEvent = input
                                             .newScanDatabaseRequest(result.getStartMarker())
                                             .createNewEventEntry(EVENT_BUS_NAME, DETAIL_TYPE,
                                                                  context.getInvokedFunctionArn());
        sendEvent(newEvent);
    }

    private void sendEvent(PutEventsRequestEntry putEventRequestEntry) {
        PutEventsRequest putEventRequest = PutEventsRequest.builder()
                                               .entries(putEventRequestEntry)
                                               .build();
        eventBridgeClient.putEvents(putEventRequest);
    }
}
