package no.unit.nva.publication.s3imports;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.core.attempt.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqsBatchMessenger {

    public static final int MAX_NUMBER_OF_MESSAGES_PER_BATCH_ALLOWED_BY_AWS = 10;
    private static final Logger logger = LoggerFactory.getLogger(SqsBatchMessenger.class);
    private final AmazonSQS client;
    private final String queue;

    private final Map<String, EventReference> requestIdToMessageBody;

    public SqsBatchMessenger(AmazonSQS client, String queue) {
        this.client = client;
        this.queue = queue;
        this.requestIdToMessageBody = new HashMap<>();
    }

    public PutSqsMessageResult sendMessages(List<EventReference> messageBodies) {
        var messageRequestEntries = createMessageRequestEntries(messageBodies);
        var batches = batchEventReferences(messageRequestEntries);
        var batchRequest = batches.stream().map(this::mapToSendMessageBatchRequestEntry);
        return batchRequest.map(this::sendToSqs).reduce(PutSqsMessageResult::combine).orElseThrow();
    }

    private PutSqsMessageResult sendToSqs(SendMessageBatchRequest request) {
        return attempt(() -> client.sendMessageBatch(request)).map(this::createPutSqsMessageResult)
                   .orElse(failure -> mapToEverythingFailed(failure, request));
    }

    private PutSqsMessageResult mapToEverythingFailed(Failure<PutSqsMessageResult> failure,
                                                      SendMessageBatchRequest request) {
        logger.error("Exception when sending message to SQS queue: " + failure.getException().getMessage());
        var result = new PutSqsMessageResult();
        result.setFailures(request.getEntries()
                               .stream()
                               .map(SendMessageBatchRequestEntry::getId)
                               .map(requestIdToMessageBody::get)
                               .map(reference ->
                                        new PutSqsMessageResultFailureEntry(reference,
                                                                            failure.getException().toString()))
                               .collect(
                                   Collectors.toList()));
        return result;
    }

    private PutSqsMessageResult createPutSqsMessageResult(SendMessageBatchResult response) {
        var result = new PutSqsMessageResult();
        var failures =
            response.getFailed().stream()
                .map(s -> new PutSqsMessageResultFailureEntry(requestIdToMessageBody.get(s.getId()), s.getMessage()))
                .collect(
                Collectors.toList());
        result.setFailures(failures);
        result.setSuccesses(mapToEventReferences(getSuccesses(response)));
        return result;
    }

    private Stream<String> getSuccesses(SendMessageBatchResult response) {
        return response.getSuccessful().stream().map(SendMessageBatchResultEntry::getId);
    }

    private List<EventReference> mapToEventReferences(Stream<String> requestIds) {
        return requestIds.map(requestIdToMessageBody::get).collect(Collectors.toList());
    }

    private SendMessageBatchRequest mapToSendMessageBatchRequestEntry(
        List<SendMessageBatchRequestEntry> batchedEvents) {
        var sendMessageBatchRequest = new SendMessageBatchRequest();
        sendMessageBatchRequest.withEntries(batchedEvents);
        sendMessageBatchRequest.withQueueUrl(queue);
        return sendMessageBatchRequest;
    }

    private Collection<List<SendMessageBatchRequestEntry>> batchEventReferences(
        Stream<SendMessageBatchRequestEntry> eventReferenceStream) {
        var counter = new AtomicInteger();
        return eventReferenceStream.collect(
                Collectors.groupingBy(it -> counter.getAndIncrement() / MAX_NUMBER_OF_MESSAGES_PER_BATCH_ALLOWED_BY_AWS))
                   .values();
    }

    private Stream<SendMessageBatchRequestEntry> createMessageRequestEntries(List<EventReference> messages) {
        return messages.stream().map(this::toMessageRequestEntry);
    }

    private SendMessageBatchRequestEntry toMessageRequestEntry(EventReference message) {
        var requestId = SortableIdentifier.next().toString();
        requestIdToMessageBody.put(requestId, message);
        var sendMessageBatchRequestEntry = new SendMessageBatchRequestEntry();
        sendMessageBatchRequestEntry.setMessageBody(message.toJsonString());
        sendMessageBatchRequestEntry.setId(requestId);
        return sendMessageBatchRequestEntry;
    }
}
