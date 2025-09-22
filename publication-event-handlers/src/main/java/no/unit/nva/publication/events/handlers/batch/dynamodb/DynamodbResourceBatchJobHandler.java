package no.unit.nva.publication.events.handlers.batch.dynamodb;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.publication.events.handlers.batch.dynamodb.jobs.ReindexRecordJob;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamodbResourceBatchJobHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final Logger logger = LoggerFactory.getLogger(DynamodbResourceBatchJobHandler.class);
    private static final String ERROR_MESSAGE_ATTRIBUTE = "ErrorMessage";
    private static final String ERROR_COUNT_ATTRIBUTE = "ErrorCount";
    private static final String LAST_ERROR_TIMESTAMP_ATTRIBUTE = "LastErrorTimestamp";
    private static final String STACK_TRACE_ATTRIBUTE = "StackTrace";
    private static final String MESSAGE_BODY_ATTRIBUTE = "MessageBody";
    private static final DynamodbResourceBatchJobExecutor[] JOBS = {new ReindexRecordJob()};

    private final Map<String, DynamodbResourceBatchJobExecutor> jobHandlers;

    @JacocoGenerated
    public DynamodbResourceBatchJobHandler() {
        this(initializeDefaultJobHandlers());
    }

    public DynamodbResourceBatchJobHandler(Map<String, DynamodbResourceBatchJobExecutor> jobHandlers) {
        this.jobHandlers = jobHandlers;
    }

    @JacocoGenerated
    private static Map<String, DynamodbResourceBatchJobExecutor> initializeDefaultJobHandlers() {
        return Stream.of(JOBS).collect(Collectors.toMap(
            DynamodbResourceBatchJobExecutor::getJobType,
            Function.identity()
        ));
    }

    @Override
    public SQSBatchResponse handleRequest(SQSEvent sqsEvent, Context context) {
        logger.info("Processing batch of {} messages", sqsEvent.getRecords().size());

        var messagesByJobType = new HashMap<String, List<MessageWithWorkItem>>();
        var parseFailures = new ArrayList<SQSBatchResponse.BatchItemFailure>();

        for (var message : sqsEvent.getRecords()) {
            try {
                var workItem = parseWorkItem(message.getBody());
                messagesByJobType.computeIfAbsent(workItem.jobType(), k -> new ArrayList<>())
                    .add(new MessageWithWorkItem(message, workItem));
            } catch (Exception e) {
                logger.error("Failed to parse message: {}", message.getMessageId(), e);
                parseFailures.add(createBatchItemFailure(message, e));
            }
        }

        var executionFailures = messagesByJobType.entrySet().stream()
            .flatMap(entry -> processBatch(entry.getKey(), entry.getValue()).stream())
            .toList();

        var allFailures = new ArrayList<SQSBatchResponse.BatchItemFailure>();
        allFailures.addAll(parseFailures);
        allFailures.addAll(executionFailures);

        logger.info("Processed batch with {} failures out of {} messages",
                    allFailures.size(), sqsEvent.getRecords().size());

        return SQSBatchResponse.builder()
                   .withBatchItemFailures(allFailures)
                   .build();
    }

    private SQSBatchResponse.BatchItemFailure createBatchItemFailure(SQSMessage message, Exception exception) {
        var failure = new SQSBatchResponse.BatchItemFailure(message.getMessageId());

        var errorAttributes = new HashMap<String, String>();
        errorAttributes.put(ERROR_MESSAGE_ATTRIBUTE, exception.getMessage());
        errorAttributes.put(MESSAGE_BODY_ATTRIBUTE, message.getBody());
        var stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        errorAttributes.put(STACK_TRACE_ATTRIBUTE, stringWriter.toString());
        errorAttributes.put(ERROR_COUNT_ATTRIBUTE, getErrorCount(message));
        errorAttributes.put(LAST_ERROR_TIMESTAMP_ATTRIBUTE, String.valueOf(System.currentTimeMillis()));

        logger.info("Message {} failed with attributes: {}", message.getMessageId(), errorAttributes);

        return failure;
    }

    private String getErrorCount(SQSMessage message) {
        return Optional.ofNullable(message.getAttributes())
                   .map(attrs -> attrs.get("ApproximateReceiveCount"))
                   .orElse("1");
    }

    private List<SQSBatchResponse.BatchItemFailure> processBatch(String jobType, List<MessageWithWorkItem> messages) {
        logger.info("Processing batch of {} messages for job type: {}", messages.size(), jobType);
        
        var executor = jobHandlers.get(jobType);
        if (isNull(executor)) {
            logger.error("No executor found for job type: {}", jobType);
            return messages.stream()
                .map(m -> createBatchItemFailure(m.message, 
                    new UnsupportedOperationException("Unsupported job type: " + jobType)))
                .toList();
        }

        try {
            var workItems = messages.stream()
                .map(m -> m.workItem)
                .toList();

            executor.executeBatch(workItems);

            logger.info("Successfully processed batch of {} items for job type: {}", workItems.size(), jobType);
            return List.of();

        } catch (Exception e) {
            logger.error("Failed to process batch for job type: {}", jobType, e);
            return messages.stream()
                .map(m -> createBatchItemFailure(m.message, e))
                .toList();
        }
    }

    private BatchWorkItem parseWorkItem(String body) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(body, BatchWorkItem.class);
    }

    private record MessageWithWorkItem(SQSMessage message, BatchWorkItem workItem) {
    }
}