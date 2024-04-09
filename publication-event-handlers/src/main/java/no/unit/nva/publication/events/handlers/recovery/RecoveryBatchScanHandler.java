package no.unit.nva.publication.events.handlers.recovery;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import no.unit.nva.events.handlers.EventHandler;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.handlers.expandresources.RecoveryEntry;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.ResourceQueueClient;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.sqs.model.Message;

public class RecoveryBatchScanHandler extends EventHandler<RecoveryEventRequest, Void> {

    public static final String RECOVERY_QUEUE = new Environment().readEnv("RECOVERY_QUEUE");
    public static final String ID = "id";
    public static final String ENTRIES_PROCEEDED_MESSAGE = "{} entries have been successfully processed";
    public static final String EMITTING_NEW_EVENT = "Emitting new event";
    public static final String TYPE = "type";
    private static final Logger logger = LoggerFactory.getLogger(RecoveryBatchScanHandler.class);
    private final EventBridgeClient eventBridgeClient;
    private final QueueClient queueClient;
    private final ResourceService resourceService;
    private final TicketService ticketService;
    private final MessageService messageService;

    @JacocoGenerated
    public RecoveryBatchScanHandler() {
        this(ResourceService.defaultService(), TicketService.defaultService(), MessageService.defaultService(),
             ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE), EventBridgeClient.create());
    }

    public RecoveryBatchScanHandler(ResourceService resourceService, TicketService ticketService,
                                    MessageService messageService, QueueClient queueClient,
                                    EventBridgeClient eventBridgeClient) {
        super(RecoveryEventRequest.class);
        this.resourceService = resourceService;
        this.ticketService = ticketService;
        this.messageService = messageService;
        this.queueClient = queueClient;
        this.eventBridgeClient = eventBridgeClient;
    }

    @Override
    protected Void processInput(RecoveryEventRequest recoveryRequest,
                                AwsEventBridgeEvent<RecoveryEventRequest> awsEventBridgeEvent, Context context) {

        var messages = queueClient.readMessages();

        processMessages(messages);

        if (moreMessagesOnQueue(messages)) {
            emitNewRecoveryEvent(context);
        }

        return null;
    }

    private static boolean moreMessagesOnQueue(List<Message> messages) {
        return !messages.isEmpty();
    }

    private static SortableIdentifier extractResourceIdentifier(Message message) {
        return new SortableIdentifier(message.messageAttributes().get(ID).stringValue());
    }

    private static String extractType(Message message) {
        return message.messageAttributes().get(TYPE).stringValue();
    }

    private void processMessages(List<Message> messages) {
        messages.forEach(this::refreshEntry);
        queueClient.deleteMessages(messages);
        logger.info(ENTRIES_PROCEEDED_MESSAGE, messages.size());
    }

    private void refreshEntry(Message message) {
        var identifier = extractResourceIdentifier(message);
        var type = extractType(message);

        switch (type) {
            case RecoveryEntry.RESOURCE:
                resourceService.refresh(identifier);
                break;
            case RecoveryEntry.TICKET:
                ticketService.refresh(identifier);
                break;
            case RecoveryEntry.MESSAGE:
                messageService.refresh(identifier);
                break;
            default:
                break;
        }
    }

    private void emitNewRecoveryEvent(Context context) {
        var eventEntry = RecoveryEventRequest.createNewEntry(context);
        eventBridgeClient.putEvents(PutEventsRequest.builder().entries(eventEntry).build());
        logger.info(EMITTING_NEW_EVENT);
    }
}
