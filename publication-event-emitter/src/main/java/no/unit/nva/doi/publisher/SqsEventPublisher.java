package no.unit.nva.doi.publisher;

import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

public class SqsEventPublisher implements EventPublisher {

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final Logger logger = LoggerFactory.getLogger(SqsEventPublisher.class);

    private final SqsClient sqs;
    private final String queueUrl;

    public SqsEventPublisher(SqsClient sqs, String queueUrl) {
        this.sqs = sqs;
        this.queueUrl = queueUrl;
    }

    @Override
    public void publish(final DynamodbEvent event) {
        logger.debug("Sending events {} to SQS queue {}", event, queueUrl);
        try {
            SendMessageRequest message = createSendMessageRequest(event);
            sqs.sendMessage(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private SendMessageRequest createSendMessageRequest(DynamodbEvent event) throws JsonProcessingException {
        return SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(objectMapper.writeValueAsString(event))
            .build();
    }
}
