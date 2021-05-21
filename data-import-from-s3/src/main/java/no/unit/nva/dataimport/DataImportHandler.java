package no.unit.nva.dataimport;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ConsumedCapacity;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.JsonUtils;
import nva.commons.core.parallel.ParallelExecutionException;
import nva.commons.core.parallel.ParallelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for importing data from an S3 bucket into a DynamoDb table. Data in S3 are expected to be in Amazon's ION
 * format.
 */
public class DataImportHandler {

    public static final S3Driver SETUP_DRIVER_AFTER_BUCKET_IS_KNOWN = null;
    public static final String EMPTY_LIST_ERROR = "Specified folder either does not exist or is empty";
    public static final int ONE_OF_THE_ENTRIES_WITH_COMMON_KEY = 0;
    private static final int MAX_ATTEMPTS = 10;
    private static final Logger logger = LoggerFactory.getLogger(DataImportHandler.class);
    private final AmazonDynamoDB dynamoClient;
    private S3Driver s3Driver;
    private String tableName;

    @JacocoGenerated
    public DataImportHandler() {
        this(SETUP_DRIVER_AFTER_BUCKET_IS_KNOWN, defaultDynamoClient());
    }

    public DataImportHandler(S3Driver s3Driver, AmazonDynamoDB dynamoClient) {
        this.s3Driver = s3Driver;
        this.dynamoClient = dynamoClient;
    }

    public List<ImportResult<FailedDynamoEntriesReport>> handleRequest(Map<String, String> request) {
        logger.info("Request: " + requestToJson(request));
        ImportRequest input = ImportRequest.fromMap(request);
        tableName = input.getTable();

        setupS3Driver(input.extractBucketFromS3Location());
        List<String> filenames = fetchFilenamesFromS3Location(input);

        List<ImportResult<FailedDynamoEntriesReport>> importResults = attempt(
            () -> insertAllFiles(filenames)).orElseThrow();

        logResults(importResults);
        //Return array because AWS Serializer has problem with lists.
        return importResults;
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoClient() {
        return AmazonDynamoDBClient.builder().build();
    }

    private static ImportResult<FailedDynamoEntriesReport> generateReportEntry(ParallelExecutionException failure) {
        String message = failure.getMessage();
        String inputFilename = (String) failure.getInput();
        FailedDynamoEntriesReport resultReport = new FailedDynamoEntriesReport(message, inputFilename);
        return ImportResult.reportFailure(resultReport, failure);
    }

    private String requestToJson(Map<String, String> request) {
        return attempt(() -> JsonUtils.objectMapper.writeValueAsString(request)).orElseThrow();
    }

    private List<String> fetchFilenamesFromS3Location(ImportRequest input) {
        List<String> filenames = s3Driver.listFiles(Path.of(input.extractPathFromS3Location()));

        if (filenames.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LIST_ERROR);
        }
        return filenames;
    }

    private List<ImportResult<FailedDynamoEntriesReport>> insertAllFiles(List<String> filenames)
        throws InterruptedException {
        List<String> filesToInsert = filenames;
        List<ImportResult<FailedDynamoEntriesReport>> failedImports = Collections.emptyList();

        int attempts = 0;
        while (!filesToInsert.isEmpty() && attempts < MAX_ATTEMPTS) {
            ParallelMapper<String, BatchWriteItemResult> mapping = insertFilesToDynamo(filesToInsert);

            failedImports = collectFilesWithFailures(mapping.getExceptions());
            filesToInsert = extractFilenamesFromFailedImports(failedImports);
            attempts++;
        }
        return failedImports;
    }

    private ParallelMapper<String, BatchWriteItemResult> insertFilesToDynamo(List<String> filesToInsert)
        throws InterruptedException {
        return new ParallelMapper<>(filesToInsert, this::insertFileToDynamo).map();
    }

    private List<String> extractFilenamesFromFailedImports(
        List<ImportResult<FailedDynamoEntriesReport>> failedImports) {
        return failedImports.stream().parallel()
                   .map(ImportResult::getInput)
                   .map(FailedDynamoEntriesReport::getInputFilename)
                   .collect(Collectors.toList());
    }

    private void setupS3Driver(String bucketName) {
        if (isNull(s3Driver)) {
            s3Driver = new S3Driver(bucketName);
        }
    }

    private List<ImportResult<FailedDynamoEntriesReport>> collectFilesWithFailures(
        List<ParallelExecutionException> failures) {
        return failures.stream()
                   .map(DataImportHandler::generateReportEntry)
                   .collect(Collectors.toList());
    }

    private BatchWriteItemResult insertFileToDynamo(String filename) {
        S3ToDynamoImporter s3ToDynamoImporter = new S3ToDynamoImporter(dynamoClient, s3Driver, tableName, filename);
        List<BatchWriteItemResult> results = attempt(s3ToDynamoImporter::writeFileToDynamo).orElseThrow();
        BatchWriteItemResult result = collectResults(results);
        if (itemsFailedToBeInserted(result)) {
            throw new BatchInsertionFailureException(result);
        }
        return result;
    }

    private boolean itemsFailedToBeInserted(BatchWriteItemResult result) {
        return !result.getUnprocessedItems().isEmpty();
    }

    private BatchWriteItemResult collectResults(List<BatchWriteItemResult> results) {
        return results.stream()
                   .reduce(this::mergeResults)
                   .orElse(new BatchWriteItemResult());
    }

    private void logResults(List<ImportResult<FailedDynamoEntriesReport>> importResults) {
        String resultJson = attempt(() -> JsonUtils.objectMapper.writeValueAsString(importResults)).orElseThrow();
        logger.info("result:" + resultJson);
    }

    private BatchWriteItemResult mergeResults(BatchWriteItemResult left, BatchWriteItemResult right) {
        Map<String, List<WriteRequest>> leftUnprocessedItems = extractUnprocessedItems(left);
        Map<String, List<WriteRequest>> rightUnprocessedItems = extractUnprocessedItems(right);
        Map<String, List<WriteRequest>> unprocessedItems = mergeMapValues(leftUnprocessedItems, rightUnprocessedItems);

        List<ConsumedCapacity> leftConsumedCapacity = extractConsumedCapacity(left);
        List<ConsumedCapacity> rightConsumedCapacity = extractConsumedCapacity(right);
        List<ConsumedCapacity> mergedConsumedCapacity = new ArrayList<>(leftConsumedCapacity);

        mergedConsumedCapacity.addAll(rightConsumedCapacity);

        return new BatchWriteItemResult()
                   .withUnprocessedItems(unprocessedItems)
                   .withConsumedCapacity(mergedConsumedCapacity);
    }

    private List<ConsumedCapacity> extractConsumedCapacity(BatchWriteItemResult left) {
        return Optional.ofNullable(left)
                   .map(BatchWriteItemResult::getConsumedCapacity)
                   .orElse(Collections.emptyList());
    }

    private Map<String, List<WriteRequest>> extractUnprocessedItems(BatchWriteItemResult left) {
        return Optional.ofNullable(left)
                   .map(BatchWriteItemResult::getUnprocessedItems)
                   .orElse(Collections.emptyMap());
    }

    /**
     * This method accepts two Maps whose values are Lists and returns a Map where the value for each key is the
     * concatenation of the values of the input maps for the respective keys.
     *
     * @param left  The first input.
     * @param right The second input.
     * @param <T>   The class of the objects of the lists.
     * @return a Map with merged values.
     */
    private <T> Map<String, List<T>> mergeMapValues(Map<String, List<T>> left, Map<String, List<T>> right) {
        return Stream.concat(left.entrySet().stream(), right.entrySet().stream())
                   .collect(Collectors.groupingBy(Entry::getKey))
                   .values()
                   .stream()
                   .flatMap(this::mergeLists)
                   .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    private <T> Stream<SimpleEntry<String, List<T>>> mergeLists(List<Entry<String, List<T>>> entries) {
        List<T> values = entries.stream().flatMap(e -> e.getValue().stream()).collect(Collectors.toList());
        return attempt(() -> entries.get(ONE_OF_THE_ENTRIES_WITH_COMMON_KEY).getKey())
                   .map(key -> new SimpleEntry<>(key, values))
                   .stream();
    }
}
