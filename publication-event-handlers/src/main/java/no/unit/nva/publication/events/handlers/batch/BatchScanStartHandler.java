package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.publication.events.handlers.ConfigurationForPushingDirectlyToEventBridge.EVENT_BUS_NAME;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import no.unit.nva.publication.events.bodies.ScanDatabaseRequest;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class BatchScanStartHandler implements RequestStreamHandler {
    
    public static final String INFORMATION_MESSAGE =
        "Starting scanning with pageSize equal to: %s. Set 'pageSize' between [1,1000] "
        + "if you want a different pageSize value.";
    private static final Logger logger = LoggerFactory.getLogger(BatchScanStartHandler.class);
    private final EventBridgeClient client;
    
    @JacocoGenerated
    public BatchScanStartHandler() {
        this(defaultClient());
    }
    
    public BatchScanStartHandler(EventBridgeClient client) {
        this.client = client;
    }
    
    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        String inputString = IoUtils.streamToString(input);
        ScanDatabaseRequest scanRequest =
            objectMapper.readValue(inputString, ScanDatabaseRequest.class);
        logger.info(String.format(INFORMATION_MESSAGE, scanRequest.getPageSize()));
        var event = scanRequest.createNewEventEntry(
            EVENT_BUS_NAME,
            EventBasedBatchScanHandler.DETAIL_TYPE,
            context.getInvokedFunctionArn());
        PutEventsResponse response = sendEvent(event);
        logger.info(response.toString());
    }
    
    @JacocoGenerated
    private static EventBridgeClient defaultClient() {
        return EventBridgeClient.builder()
                   .httpClientBuilder(UrlConnectionHttpClient.builder())
                   .build();
    }
    
    private PutEventsResponse sendEvent(PutEventsRequestEntry event) {
        PutEventsRequest putEventsRequest = PutEventsRequest.builder().entries(event).build();
        return client.putEvents(putEventsRequest);
    }
}
