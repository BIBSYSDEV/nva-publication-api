package no.unit.nva.publication.queue;

import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;

/**
 * Producer-only view of an SQS queue. Unlike {@link QueueClient}, it is not bound to a single
 * queue: the target queue is carried by each {@link SendMessageBatchRequest}, so a sender can
 * publish to any queue.
 */
public interface QueueMessageSender {

  SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest);
}
