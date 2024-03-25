package no.unit.nva.publication.events.handlers.recovery;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.ResourceQueueClient;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.sqs.model.Message;

public class RecoveryBatchScanHandler extends EventHandler<RecoveryEventRequest, Void> {

    private static final Logger logger = LoggerFactory.getLogger(RecoveryBatchScanHandler.class);
    public static final String RECOVERY_QUEUE = new Environment().readEnv("RECOVERY_QUEUE");
    public static final String ID = "id";
    public static final int MAX_NUMBER_OF_MESSAGES_PER_RECOVERY_REQUEST = 10;
    private final EventBridgeClient eventBridgeClient;
    private final QueueClient queueClient;
    private final ResourceService resourceService;

    @JacocoGenerated
    public RecoveryBatchScanHandler() {
        this(ResourceService.defaultService(), ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE),
             EventBridgeClient.create());
    }

    public RecoveryBatchScanHandler(ResourceService resourceService, QueueClient queueClient,
                                    EventBridgeClient eventBridgeClient) {
        super(RecoveryEventRequest.class);
        this.resourceService = resourceService;
        this.queueClient = queueClient;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    protected Void processInput(RecoveryEventRequest recoveryRequest,
                                AwsEventBridgeEvent<RecoveryEventRequest> awsEventBridgeEvent, Context context) {

        var messages = queueClient.readMessages();
        logger.info("Number of extracted messages: {}", messages.size());
        logger.info("Number of extracted messages: {}", messages.getFirst());
        messages.stream()
            .map(RecoveryBatchScanHandler::extractResourceIdentifier)
            .forEach(resourceService::refresh);

        queueClient.deleteMessages(messages);

        if (moreMessagesOnQueue(messages)) {
            emitNewRecoveryEvent();
        }

        return null;
    }

    private static boolean moreMessagesOnQueue(List<Message> messages) {
        return messages.size() == MAX_NUMBER_OF_MESSAGES_PER_RECOVERY_REQUEST;
    }

    private static SortableIdentifier extractResourceIdentifier(Message message) {
        return new SortableIdentifier(message.messageAttributes().get(ID).stringValue());
    }

    private void emitNewRecoveryEvent() {
        var eventEntry = RecoveryEventRequest.createNewEntry();
        eventBridgeClient.putEvents(PutEventsRequest.builder().entries(eventEntry).build());
    }
}
