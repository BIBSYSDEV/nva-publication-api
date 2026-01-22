package no.unit.nva.cristin.utils;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;

public class FakeSqsClient implements SqsClient {

    private final List<String> messageBodies;
    private final List<String> queueUrls;

    public FakeSqsClient() {
        messageBodies = new ArrayList<>();
        queueUrls = new ArrayList<>();
    }

    public List<String> getQueueUrls() {
        return queueUrls;
    }

    public List<String> getMessageBodies() {
        return messageBodies;
    }

    @Override
    public SendMessageBatchResponse sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        var messages = sendMessageBatchRequest.entries()
                           .stream()
                           .map(SendMessageBatchRequestEntry::messageBody)
                           .toList();
        messageBodies.addAll(messages);
        queueUrls.add(sendMessageBatchRequest.queueUrl());
        var messageBatchResultEntries = sendMessageBatchRequest.entries()
                                            .stream()
                                            .map(this::convertToResult)
                                            .toList();
        return SendMessageBatchResponse.builder()
                   .successful(messageBatchResultEntries)
                   .build();
    }

    @Override
    public String serviceName() {
        return "sqs";
    }

    @Override
    public void close() {
        // No-op for fake client
    }

    private SendMessageBatchResultEntry convertToResult(SendMessageBatchRequestEntry sendMessageBatchRequestEntry) {
        return SendMessageBatchResultEntry.builder()
                   .id(sendMessageBatchRequestEntry.id())
                   .messageId(sendMessageBatchRequestEntry.id())
                   .build();
    }
}
