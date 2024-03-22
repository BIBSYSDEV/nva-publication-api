package no.unit.nva.publication.queue;

import java.time.Duration;
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

    public static final int MAXIMUM_NUMBER_OF_MESSAGES = 10;
    public static final String AWS_REGION = "AWS_REGION";
    private static final int MAX_CONNECTIONS = 10_000;
    private static final long IDLE_TIME = 30;
    private static final long TIMEOUT_TIME = 30;
    private final SqsClient sqsClient;

    private ResourceQueueClient(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
    }

    public static ResourceQueueClient defaultResourceQueueClient() {
        return new ResourceQueueClient(defaultClient());
    }

    @Override
    public void sendMessage(SendMessageRequest sendMessageRequest) {
        sqsClient.sendMessage(sendMessageRequest);
    }

    @Override
    public List<Message> readMessages(String queue) {
        var receiveMessageRequest = ReceiveMessageRequest.builder()
                                        .queueUrl(queue)
                                        .maxNumberOfMessages(MAXIMUM_NUMBER_OF_MESSAGES)
                                        .build();
        return sqsClient.receiveMessage(receiveMessageRequest).messages();
    }

    @Override
    public void deleteMessages(String queue, List<Message> messages) {
        var entriesToDelete = messages.stream().map(ResourceQueueClient::toDeleteMessageRequest).toList();
        var deleteMessagesBatchRequest = DeleteMessageBatchRequest.builder()
                                             .queueUrl(queue)
                                             .entries(entriesToDelete)
                                             .build();
        sqsClient.deleteMessageBatch(deleteMessagesBatchRequest);
    }

    private static SqsClient defaultClient() {
        return SqsClient.builder().region(getRegion()).httpClient(httpClientForConcurrentQueries()).build();
    }

    private static DeleteMessageBatchRequestEntry toDeleteMessageRequest(Message message) {
        return DeleteMessageBatchRequestEntry.builder().id(message.messageId()).build();
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
}
