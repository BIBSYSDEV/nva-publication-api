package no.unit.nva.publication.queue;

import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Queue-agnostic batch producer: the target queue rides on each {@link SendMessageBatchRequest}, so
 * one sender can publish to any queue. Contrast with {@link QueueClient}, which adds queue-bound
 * read/delete operations for consuming a single queue.
 */
public interface QueueMessageSender {

  SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest);
}
