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
import no.unit.nva.s3.S3Driver;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.parallel.ParallelExecutionException;
import nva.commons.core.parallel.ParallelMapper;

public class DataImport {

    private static final int MAX_ATTEMPTS = 10;
    private final AmazonDynamoDB dynamoClient;
    private S3Driver s3Driver;
    private String tableName;

    @JacocoGenerated
    public DataImport() {
        this(null, defaultDynamoClient());
    }

    public DataImport(S3Driver s3Driver, AmazonDynamoDB dynamoClient) {
        this.s3Driver = s3Driver;
        this.dynamoClient = dynamoClient;
    }

    public List<ImportResult> importAllFilesInFolder(ImportRequest input) {
        tableName = input.getTable();
        setupS3Driver(input.getBucketName());
        List<String> filenames = s3Driver.listFiles(Path.of(input.getFolderPath()));
        return attempt(() -> insertAllFiles(filenames)).orElseThrow();
    }

    @JacocoGenerated
    private static AmazonDynamoDB defaultDynamoClient() {
        return AmazonDynamoDBClient.builder().build();
    }

    private List<ImportResult> insertAllFiles(List<String> filenames) throws InterruptedException {
        List<String> filesToInsert = filenames;
        List<ImportResult> failedImports = Collections.emptyList();
        int attempts = 0;
        while (!filesToInsert.isEmpty() && attempts < MAX_ATTEMPTS) {
            ParallelMapper<String, BatchWriteItemResult> mapping =
                new ParallelMapper<>(filesToInsert, this::insertFile, ParallelMapper.DEFAULT_BATCH_SIZE).map();

            failedImports = collectFilesWithFailures(mapping.getExceptions());
            filesToInsert = extractFilenamesFromFailedImports(failedImports);
            attempts++;
        }
        return failedImports;
    }

    private List<String> extractFilenamesFromFailedImports(List<ImportResult> failedImports) {
        return failedImports.stream().parallel()
                   .map(ImportResult::getFilename)
                   .collect(Collectors.toList());
    }

    private void setupS3Driver(String bucketName) {
        if (isNull(s3Driver)) {
            s3Driver = new S3Driver(bucketName);
        }
    }

    private List<ImportResult> collectFilesWithFailures(List<ParallelExecutionException> failures) {
        return failures.stream()
                   .map(ImportResult::fromParallelExecutionException)
                   .collect(Collectors.toList());
    }

    private BatchWriteItemResult insertFile(String filename) {
        FileImporter fileImporter = new FileImporter(dynamoClient, s3Driver, tableName, filename);
        List<BatchWriteItemResult> results = attempt(fileImporter::insertFileToDynamo).orElseThrow();
        BatchWriteItemResult result = results.stream().reduce(this::mergeResults).orElse(new BatchWriteItemResult());
        if (itemsFailedToBeInserted(result)) {
            throw new BatchInsertionFailureException(result);
        }
        return result;
    }

    private boolean itemsFailedToBeInserted(BatchWriteItemResult result) {
        return !result.getUnprocessedItems().isEmpty();
    }

    private BatchWriteItemResult mergeResults(BatchWriteItemResult left, BatchWriteItemResult right) {
        Map<String, List<WriteRequest>> leftUnprocessedItems = extractUnprocessedItems(left);
        Map<String, List<WriteRequest>> rightUnprocessedItems = extractUnprocessedItems(right);
        Map<String, List<WriteRequest>> unprocessedItems = mergeMaps(leftUnprocessedItems, rightUnprocessedItems);

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

    private <T> Map<String, List<T>> mergeMaps(Map<String, List<T>> left, Map<String, List<T>> right) {
        return Stream.concat(left.entrySet().stream(), right.entrySet().stream())
                   .collect(Collectors.groupingBy(Entry::getKey))
                   .values()
                   .stream()
                   .flatMap(this::mergeLists)
                   .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));
    }

    private <T> Stream<SimpleEntry<String, List<T>>> mergeLists(List<Entry<String, List<T>>> entries) {
        List<T> values = entries.stream().flatMap(e -> e.getValue().stream()).collect(Collectors.toList());
        return attempt(() -> entries.get(0).getKey())
                   .map(key -> new SimpleEntry<>(key, values))
                   .stream();
    }
}
