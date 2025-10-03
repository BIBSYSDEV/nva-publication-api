package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_KEY_PAIRS;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
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
import no.unit.nva.publication.events.handlers.batch.dynamodb.jobs.MigrateResourceJob;
import no.unit.nva.publication.events.handlers.batch.dynamodb.jobs.NoGsiResultsException;
import no.unit.nva.publication.events.handlers.batch.dynamodb.jobs.ReindexRecordJob;
import nva.commons.core.Environment;
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
    private static final String TABLE_NAME_ENV = "TABLE_NAME";

    private static final DynamodbResourceBatchJobExecutor[] JOBS = {new ReindexRecordJob(), new MigrateResourceJob()};
    private static final String DEFAULT_TO_ONE_ITEM = "1";
    private static final String APPROXIMATE_RECEIVE_COUNT = "ApproximateReceiveCount";

    private final Map<String, DynamodbResourceBatchJobExecutor> jobHandlers;
    private final AmazonDynamoDB client;
    private final String tableName;

    @JacocoGenerated
    public DynamodbResourceBatchJobHandler() {
        this(initializeDefaultJobHandlers(), AmazonDynamoDBClientBuilder.defaultClient(),
             new Environment().readEnv(TABLE_NAME_ENV));
    }

    public DynamodbResourceBatchJobHandler(Map<String, DynamodbResourceBatchJobExecutor> jobHandlers,
                                           AmazonDynamoDB amazonDynamoDB, String tableName) {
        this.jobHandlers = jobHandlers;
        this.client = amazonDynamoDB;
        this.tableName = tableName;
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
                   .map(attrs -> attrs.get(APPROXIMATE_RECEIVE_COUNT))
                   .orElse(DEFAULT_TO_ONE_ITEM);
    }

    private List<SQSBatchResponse.BatchItemFailure> processBatch(String jobType, List<MessageWithWorkItem> messages) {
        logger.info("Processing batch of {} messages for job type: {}", messages.size(), jobType);

        var executor = jobHandlers.get(jobType);
        if (isNull(executor)) {
            logger.error("No executor found for job type: {}", jobType);
            return messages.stream()
                       .map(messageWithWorkItem -> createBatchItemFailure(messageWithWorkItem.message,
                                                                          new UnsupportedOperationException(
                                                                              "Unsupported job type: " + jobType)))
                       .toList();
        }

        try {
            var workItems = messages.stream()
                                .map(messageWithWorkItem -> messageWithWorkItem.workItem)
                                .flatMap(this::resolvePrimaryBatchWorkItemsConditional)
                                .toList();

            executor.executeBatch(workItems);

            logger.info("Successfully processed batch of {} items for job type: {}", workItems.size(), jobType);
            return emptyList();
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

    private Stream<BatchWorkItem> resolvePrimaryBatchWorkItemsConditional(BatchWorkItem workItem) {
        if (workItem.dynamoDbKey().isGsiQuery()) {
            return resolvePrimaryKey(workItem);
        }

        return Stream.of(workItem);
    }

    private Stream<BatchWorkItem> resolvePrimaryKey(BatchWorkItem gsiItem) {
        var resolvedItems = fetchAllItemsByGsi(gsiItem);

        if (resolvedItems.isEmpty()) {
            throw new NoGsiResultsException(gsiItem.dynamoDbKey());
        }

        logger.info("Resolved {} primary keys from GSI query for index: {}",
                    resolvedItems.size(), gsiItem.dynamoDbKey().indexName());
        return resolvedItems.stream();
    }

    private List<BatchWorkItem> fetchAllItemsByGsi(BatchWorkItem gsiItem) {
        var queryRequest = createGsiQueryRequest(gsiItem.dynamoDbKey());
        try {
            var result = client.query(queryRequest);

            var resolvedItems = new ArrayList<>(createWorkItems(gsiItem, result));

            while (nonNull(result.getLastEvaluatedKey()) && !result.getLastEvaluatedKey().isEmpty()) {
                queryRequest.setExclusiveStartKey(result.getLastEvaluatedKey());
                result = client.query(queryRequest);

                resolvedItems.addAll(createWorkItems(gsiItem, result));
            }
            return resolvedItems;
        } catch (Exception e) {
            logger.error("Failed to resolve GSI to primary keys for index: {}", gsiItem.dynamoDbKey().indexName(), e);
            throw new RuntimeException("Failed to resolve GSI to primary keys", e);
        }
    }

    private QueryRequest createGsiQueryRequest(DynamodbResourceBatchDynamoDbKey key) {
        var gsiKey = GSI_KEY_PAIRS.get(key.indexName());

        return new QueryRequest()
                   .withTableName(tableName)
                   .withIndexName(key.indexName())
                   .withKeyConditionExpression("#pk = :pkval AND #sk = :skval")
                   .withExpressionAttributeNames(Map.of(
                       "#pk", gsiKey.partitionKey(),
                       "#sk", gsiKey.sortKey()
                   ))
                   .withExpressionAttributeValues(Map.of(
                       ":pkval", new AttributeValue().withS(key.partitionKey()),
                       ":skval", new AttributeValue().withS(key.sortKey())
                   ))
                   .withProjectionExpression("PK0, SK0")
                   .withLimit(100);
    }

    private static List<BatchWorkItem> createWorkItems(BatchWorkItem gsiItem, QueryResult result) {
        return result.getItems().stream()
                   .map(item -> createPrimaryKeyWorkFromGsi(gsiItem, item))
                   .toList();
    }

    private static BatchWorkItem createPrimaryKeyWorkFromGsi(BatchWorkItem gsiItem, Map<String, AttributeValue> item) {
        var primaryPk = item.get(PRIMARY_KEY_PARTITION_KEY_NAME).getS();
        var primarySk = item.get(PRIMARY_KEY_SORT_KEY_NAME).getS();
        var primaryKey = new DynamodbResourceBatchDynamoDbKey(primaryPk, primarySk);
        return new BatchWorkItem(primaryKey, gsiItem.jobType(), gsiItem.parameters());
    }

    private record MessageWithWorkItem(SQSMessage message, BatchWorkItem workItem) {

    }
}