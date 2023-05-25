package no.unit.nva.publication.s3imports.utils;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AddPermissionRequest;
import com.amazonaws.services.sqs.model.AddPermissionResult;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityBatchResult;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityResult;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageBatchResult;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteMessageResult;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.DeleteQueueResult;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesResult;
import com.amazonaws.services.sqs.model.GetQueueUrlRequest;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesRequest;
import com.amazonaws.services.sqs.model.ListDeadLetterSourceQueuesResult;
import com.amazonaws.services.sqs.model.ListQueueTagsRequest;
import com.amazonaws.services.sqs.model.ListQueueTagsResult;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.ListQueuesResult;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.PurgeQueueResult;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.RemovePermissionRequest;
import com.amazonaws.services.sqs.model.RemovePermissionResult;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.amazonaws.services.sqs.model.SendMessageBatchResultEntry;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.amazonaws.services.sqs.model.SetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.SetQueueAttributesResult;
import com.amazonaws.services.sqs.model.TagQueueRequest;
import com.amazonaws.services.sqs.model.TagQueueResult;
import com.amazonaws.services.sqs.model.UntagQueueRequest;
import com.amazonaws.services.sqs.model.UntagQueueResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FakeAmazonSQS implements AmazonSQS {

    private List<String> messageBodies;
    private List<String> queueUrls;

    public FakeAmazonSQS() {
        messageBodies = new ArrayList<>();
        queueUrls = new ArrayList<>();
    }

    public List<String> getQueueUrls() {
        return queueUrls;
    }

    @Override
    public void setEndpoint(String endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setRegion(Region region) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AddPermissionResult addPermission(AddPermissionRequest addPermissionRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AddPermissionResult addPermission(String queueUrl, String label, List<String> aWSAccountIds,
                                             List<String> actions) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChangeMessageVisibilityResult changeMessageVisibility(
        ChangeMessageVisibilityRequest changeMessageVisibilityRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChangeMessageVisibilityResult changeMessageVisibility(String queueUrl, String receiptHandle,
                                                                 Integer visibilityTimeout) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(
        ChangeMessageVisibilityBatchRequest changeMessageVisibilityBatchRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChangeMessageVisibilityBatchResult changeMessageVisibilityBatch(String queueUrl,
                                                                           List<ChangeMessageVisibilityBatchRequestEntry> entries) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateQueueResult createQueue(CreateQueueRequest createQueueRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CreateQueueResult createQueue(String queueName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteMessageResult deleteMessage(DeleteMessageRequest deleteMessageRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteMessageResult deleteMessage(String queueUrl, String receiptHandle) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteMessageBatchResult deleteMessageBatch(String queueUrl, List<DeleteMessageBatchRequestEntry> entries) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteQueueResult deleteQueue(DeleteQueueRequest deleteQueueRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DeleteQueueResult deleteQueue(String queueUrl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GetQueueAttributesResult getQueueAttributes(GetQueueAttributesRequest getQueueAttributesRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GetQueueAttributesResult getQueueAttributes(String queueUrl, List<String> attributeNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GetQueueUrlResult getQueueUrl(GetQueueUrlRequest getQueueUrlRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GetQueueUrlResult getQueueUrl(String queueName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListDeadLetterSourceQueuesResult listDeadLetterSourceQueues(
        ListDeadLetterSourceQueuesRequest listDeadLetterSourceQueuesRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListQueueTagsResult listQueueTags(ListQueueTagsRequest listQueueTagsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListQueueTagsResult listQueueTags(String queueUrl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListQueuesResult listQueues(ListQueuesRequest listQueuesRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListQueuesResult listQueues() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListQueuesResult listQueues(String queueNamePrefix) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PurgeQueueResult purgeQueue(PurgeQueueRequest purgeQueueRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReceiveMessageResult receiveMessage(ReceiveMessageRequest receiveMessageRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ReceiveMessageResult receiveMessage(String queueUrl) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RemovePermissionResult removePermission(RemovePermissionRequest removePermissionRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public RemovePermissionResult removePermission(String queueUrl, String label) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SendMessageResult sendMessage(SendMessageRequest sendMessageRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SendMessageResult sendMessage(String queueUrl, String messageBody) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
        var messages = sendMessageBatchRequest.getEntries()
                           .stream()
                           .map(SendMessageBatchRequestEntry::getMessageBody)
                           .collect(Collectors.toList());
        messageBodies.addAll(messages);
        queueUrls.add(sendMessageBatchRequest.getQueueUrl());
        var messageBatchResultEntries = sendMessageBatchRequest.getEntries()
                                            .stream()
                                            .map(this::convertToResult)
                                            .collect(
                                                Collectors.toList());
        var sendMessageBatchResult = new SendMessageBatchResult();
        sendMessageBatchResult.setSuccessful(messageBatchResultEntries);
        return sendMessageBatchResult;
    }

    @Override
    public SendMessageBatchResult sendMessageBatch(String queueUrl, List<SendMessageBatchRequestEntry> entries) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SetQueueAttributesResult setQueueAttributes(SetQueueAttributesRequest setQueueAttributesRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SetQueueAttributesResult setQueueAttributes(String queueUrl, Map<String, String> attributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TagQueueResult tagQueue(TagQueueRequest tagQueueRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TagQueueResult tagQueue(String queueUrl, Map<String, String> tags) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UntagQueueResult untagQueue(UntagQueueRequest untagQueueRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UntagQueueResult untagQueue(String queueUrl, List<String> tagKeys) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        throw new UnsupportedOperationException();
    }

    public List<String> getMessageBodies() {
        return messageBodies;
    }

    private SendMessageBatchResultEntry convertToResult(SendMessageBatchRequestEntry sendMessageBatchRequestEntry) {
        var sendMessageBatchResultEntry = new SendMessageBatchResultEntry();
        sendMessageBatchResultEntry.withMessageId(sendMessageBatchRequestEntry.getId());
        return sendMessageBatchResultEntry;
    }
}
