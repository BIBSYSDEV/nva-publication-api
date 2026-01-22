package no.unit.nva.publication.s3imports;

import static java.util.stream.Collectors.groupingBy;
import static nva.commons.core.attempt.Try.attempt;
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
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;

public class SqsBatchMessenger {

    public static final int MAX_NUMBER_OF_MESSAGES_PER_BATCH_ALLOWED_BY_AWS = 10;
    private static final Logger logger = LoggerFactory.getLogger(SqsBatchMessenger.class);
    private final SqsClient client;
    private final String queue;

    private final Map<String, EventReference> requestIdToMessageBody;

    public SqsBatchMessenger(SqsClient client, String queue) {
        this.client = client;
        this.queue = queue;
        this.requestIdToMessageBody = new HashMap<>();
    }

    public PutSqsMessageResult sendMessages(List<EventReference> messageBodies) {
        var messageRequestEntries = createMessageRequestEntries(messageBodies);
        var batches = batchEventReferences(messageRequestEntries);
        return batches.stream()
                .map(this::mapToSendMessageBatchRequest)
                .map(this::sendToSqs)
                .reduce(PutSqsMessageResult::combine)
                .orElseThrow();
    }

    private PutSqsMessageResult sendToSqs(SendMessageBatchRequest request) {
        return attempt(() -> client.sendMessageBatch(request)).map(this::createPutSqsMessageResult)
                   .orElse(failure -> mapToEverythingFailed(failure, request));
    }

    private PutSqsMessageResult mapToEverythingFailed(Failure<PutSqsMessageResult> failure,
                                                      SendMessageBatchRequest request) {
        logger.error("Exception when sending message to SQS queue: " + failure.getException().getMessage());
        var result = new PutSqsMessageResult();
        result.setFailures(request.entries()
                               .stream()
                               .map(SendMessageBatchRequestEntry::id)
                               .map(requestIdToMessageBody::get)
                               .map(reference ->
                                        new PutSqsMessageResultFailureEntry(reference,
                                                                            failure.getException().toString()))
                               .collect(
                                   Collectors.toList()));
        return result;
    }

    private PutSqsMessageResult createPutSqsMessageResult(SendMessageBatchResponse response) {
        var result = new PutSqsMessageResult();
        var failures =
                response.failed().stream()
                        .map(errorEntry ->
                                new PutSqsMessageResultFailureEntry(requestIdToMessageBody.get(errorEntry.id()),
                                        errorEntry.message()))
                        .collect(
                                Collectors.toList());
        result.setFailures(failures);
        result.setSuccesses(mapToEventReferences(getSuccesses(response)));
        return result;
    }

    private Stream<String> getSuccesses(SendMessageBatchResponse response) {
        return response.successful().stream().map(SendMessageBatchResultEntry::id);
    }

    private List<EventReference> mapToEventReferences(Stream<String> requestIds) {
        return requestIds.map(requestIdToMessageBody::get).collect(Collectors.toList());
    }

    private SendMessageBatchRequest mapToSendMessageBatchRequest(
        List<SendMessageBatchRequestEntry> batchedEvents) {
        return SendMessageBatchRequest.builder()
                   .entries(batchedEvents)
                   .queueUrl(queue)
                   .build();
    }

    private Collection<List<SendMessageBatchRequestEntry>> batchEventReferences(
        Stream<SendMessageBatchRequestEntry> eventReferenceStream) {
        var counter = new AtomicInteger();
        return eventReferenceStream.collect(
                groupingBy(item -> counter.getAndIncrement() / MAX_NUMBER_OF_MESSAGES_PER_BATCH_ALLOWED_BY_AWS))
                   .values();
    }

    private Stream<SendMessageBatchRequestEntry> createMessageRequestEntries(List<EventReference> messages) {
        return messages.stream().map(this::toMessageRequestEntry);
    }

    private SendMessageBatchRequestEntry toMessageRequestEntry(EventReference message) {
        var requestId = SortableIdentifier.next().toString();
        requestIdToMessageBody.put(requestId, message);
        return SendMessageBatchRequestEntry.builder()
                   .messageBody(message.toJsonString())
                   .id(requestId)
                   .build();
    }
}
