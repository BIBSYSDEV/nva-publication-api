package no.unit.nva.publication.queue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@JacocoGenerated
public final class ResourceQueueClient implements QueueClient {

    public static final int BATCH_SIZE = 10;
    private static final String AWS_REGION = "AWS_REGION";
    private static final int MAX_CONNECTIONS = 10_000;
    private static final long IDLE_TIME = 30;
    private static final long TIMEOUT_TIME = 30;
    private static final String ALL_MESSAGE_ATTRIBUTES = "All";
    private static final int WAITING_TIME = 20;
    private final SqsClient sqsClient;
    private final String queueUrl;

    private ResourceQueueClient(SqsClient sqsClient, String queueUrl) {
        this.queueUrl = queueUrl;
        this.sqsClient = sqsClient;
    }

    public static ResourceQueueClient defaultResourceQueueClient(String queueUrl) {
        return new ResourceQueueClient(defaultClient(), queueUrl);
    }

    @Override
    public void sendMessage(SendMessageRequest sendMessageRequest) {
        sqsClient.sendMessage(sendMessageRequest);
    }

    @Override
    public List<Message> readMessages(int maximumNumberOfMessages) {
        var allMessages = new ArrayList<Message>();
        while (allMessages.size() < maximumNumberOfMessages) {
            var receiveMessageRequest = createRequest(maximumNumberOfMessages);
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            if (messages.isEmpty()) {
                break;
            }
            allMessages.addAll(messages);
        }
        return allMessages;
    }

    @Override
    public void deleteMessages(List<Message> messages) {
        var start = 0;
        while (start < messages.size()) {
            var batchToDelete = getBatchToDelete(messages, start).stream()
                                    .map(ResourceQueueClient::toDeleteMessageRequest)
                                    .toList();
            sqsClient.deleteMessageBatch(createDeleteRequest(batchToDelete));
            start += BATCH_SIZE;
        }
    }

    private static List<Message> getBatchToDelete(List<Message> messages, int start) {
        return messages.subList(start, Math.min(start + BATCH_SIZE, messages.size()));
    }

    private static SqsClient defaultClient() {
        return SqsClient.builder().region(getRegion()).httpClient(httpClientForConcurrentQueries()).build();
    }

    private static DeleteMessageBatchRequestEntry toDeleteMessageRequest(Message message) {
        return DeleteMessageBatchRequestEntry.builder()
                   .id(message.messageId())
                   .receiptHandle(message.receiptHandle())
                   .build();
    }

    private static Region getRegion() {
        return new Environment().readEnvOpt(AWS_REGION).map(Region::of).orElse(Region.EU_WEST_1);
    }

    private static SdkHttpClient httpClientForConcurrentQueries() {
        return ApacheHttpClient.builder()
                   .useIdleConnectionReaper(true)
                   .maxConnections(MAX_CONNECTIONS)
                   .connectionMaxIdleTime(Duration.ofSeconds(IDLE_TIME))
                   .connectionTimeout(Duration.ofSeconds(TIMEOUT_TIME))
                   .build();
    }

    private ReceiveMessageRequest createRequest(int maximumNumberOfMessages) {
        return ReceiveMessageRequest.builder()
                   .queueUrl(queueUrl)
                   .waitTimeSeconds(WAITING_TIME)
                   .maxNumberOfMessages(Math.min(maximumNumberOfMessages, BATCH_SIZE))
                   .messageAttributeNames(ALL_MESSAGE_ATTRIBUTES)
                   .build();
    }

    private DeleteMessageBatchRequest createDeleteRequest(List<DeleteMessageBatchRequestEntry> entriesToDelete) {
        return DeleteMessageBatchRequest.builder().queueUrl(queueUrl).entries(entriesToDelete).build();
    }
}
