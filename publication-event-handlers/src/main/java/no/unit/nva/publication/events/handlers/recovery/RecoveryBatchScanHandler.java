package no.unit.nva.publication.events.handlers.recovery;

import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.queue.QueueClient;
import no.unit.nva.publication.queue.RecoveryEntry;
import no.unit.nva.publication.queue.ResourceQueueClient;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.service.impl.VersionRefreshService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.Message;

public class RecoveryBatchScanHandler implements RequestStreamHandler {

  public static final String RECOVERY_QUEUE = new Environment().readEnv("RECOVERY_QUEUE");
  public static final String ID = "id";
  public static final String ENTRIES_PROCEEDED_MESSAGE =
      "{} entries have been successfully processed";
  public static final String TYPE = "type";
  private static final Logger logger = LoggerFactory.getLogger(RecoveryBatchScanHandler.class);
  private final QueueClient queueClient;
  private final ResourceService resourceService;
  private final TicketService ticketService;
  private final MessageService messageService;
  private final VersionRefreshService versionRefreshService;

  @JacocoGenerated
  public RecoveryBatchScanHandler() {
    this(
        ResourceService.defaultService(),
        TicketService.defaultService(),
        MessageService.defaultService(),
        ResourceQueueClient.defaultResourceQueueClient(RECOVERY_QUEUE),
        VersionRefreshService.defaultService());
  }

  public RecoveryBatchScanHandler(
      ResourceService resourceService,
      TicketService ticketService,
      MessageService messageService,
      QueueClient queueClient,
      VersionRefreshService versionRefreshService) {
    this.resourceService = resourceService;
    this.ticketService = ticketService;
    this.messageService = messageService;
    this.queueClient = queueClient;
    this.versionRefreshService = versionRefreshService;
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    var messages = queueClient.readMessages(RecoveryRequest.fromInputStream(inputStream).count());

    processMessages(messages);
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
    var entity = attempt(() -> fetch(type, identifier)).orElseThrow();
    versionRefreshService.refresh(entity);
  }

  private Entity fetch(String type, SortableIdentifier identifier) throws NotFoundException {
    return switch (type) {
      case RecoveryEntry.RESOURCE -> resourceService.getResourceByIdentifier(identifier);
      case RecoveryEntry.TICKET -> ticketService.fetchTicketByIdentifier(identifier);
      case RecoveryEntry.MESSAGE -> messageService.getMessageByIdentifier(identifier).orElseThrow();
      case RecoveryEntry.FILE ->
          FileEntry.queryObject(identifier).fetch(resourceService).orElseThrow();
      default -> throw new IllegalArgumentException("Not supported type!");
    };
  }
}
