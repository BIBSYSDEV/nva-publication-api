package no.unit.nva.dataimport;

import static no.unit.nva.dataimport.BatchWriteItemRequestMatcher.requestContains;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class DataImportHandlerTest extends ResourcesDynamoDbLocalTest {

    public static final String SOME_PATH = "somePath";

    public static final String FAILING_TO_WRITE_FILE = "inputsWithWrongSortKey.ion.gz";
    public static final String PRIMARY_KEY_LOCATOR = "PK0\\s*:\\s*(.*?)$";
    public static final String ERROR_DUE_TO_WRONG_KEY = "One of the required keys was not given a value";
    public static final String FIRST_SAMPLE = "input1.ion.gz";
    public static final String SECOND_SAMPLE = "input2.ion.gz";
    public static final String THIRD_SAMPLE = "input3.ion.gz";
    public static final String FOURTH_SAMPLE = "input4.ion.gz";
    public static final String FAILING_TO_READ_FILE = "input2.ion.gz";
    public static final String SAMPLE_WITH_SOME_FAILING_ENTRIES = "input2.ion.gz";
    public static final int PARTITION_KEY_FOR_FAILING_ENTRIES = 0;
    public static final String FAILING_ENTRIES = "expectedFailingKeysFromInput2WhenDynamoFails.txt";

    private AmazonDynamoDB dynamoDbClient;
    private String bucketName;
    private List<String> resourceFiles;

    @BeforeEach
    public void init() {
        super.init();
        this.dynamoDbClient = super.client;
        this.bucketName = "backupBucket";
        this.resourceFiles = List.of(FIRST_SAMPLE, SECOND_SAMPLE, THIRD_SAMPLE, FOURTH_SAMPLE);
    }

    @Test
    public void dataImportReadsIonFileWithResourcesAndStoresThemInDynamoDb() {
        StubS3Driver s3Driver = new StubS3Driver(bucketName, resourceFiles);
        ImportRequest request = new ImportRequest(bucketName, SOME_PATH, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);
        List<ImportResult> result = dataImportHandler.importAllFilesFromFolder(request);

        Integer itemCount = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getCount();
        assertThat(itemCount, is(equalTo(s3Driver.getAllIonItems().size())));
    }

    @Test
    public void dataImportReturnsAllFilenamesOfFailedInputsWhenReadingFromS3Fails() {
        AtomicReference<String> failingContent = new AtomicReference<>();

        StubS3Driver s3Driver = failingS3Driver(failingContent);
        ImportRequest request = new ImportRequest(bucketName, SOME_PATH, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);
        List<ImportResult> failures = dataImportHandler.importAllFilesFromFolder(request);
        List<String> failedFiles = failures.stream().map(ImportResult::getFilename).collect(Collectors.toList());
        assertThat(failedFiles, contains(FAILING_TO_READ_FILE));
    }

    @Test
    public void dataImportReturnsAllFilenamesOfFailedInputsWhenWritingToDynamoDbFails() {
        resourceFiles = List.of(FIRST_SAMPLE, FAILING_TO_WRITE_FILE);
        StubS3Driver s3Driver = new StubS3Driver(bucketName, resourceFiles);
        ImportRequest request = new ImportRequest(bucketName, SOME_PATH, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);
        List<ImportResult> failures = dataImportHandler.importAllFilesFromFolder(request);

        String failingFilename = failures.stream()
                                     .map(ImportResult::getFilename)
                                     .collect(SingletonCollector.collect());
        String errorMessage = failures.stream().map(ImportResult::getErrorMessage).collect(Collectors.joining());

        assertThat(failingFilename, is(equalTo(FAILING_TO_WRITE_FILE)));
        assertThat(errorMessage, containsString(ERROR_DUE_TO_WRONG_KEY));
    }

    @Test
    public void dataImportReturnsAllFailingPrimaryKeysOfFailedInputs() {
        resourceFiles = List.of(SAMPLE_WITH_SOME_FAILING_ENTRIES);

        List<String> expectedFailingEntries =
            attempt(() -> IoUtils.inputStreamFromResources(FAILING_ENTRIES))
                .map(stream -> new BufferedReader(new InputStreamReader(stream)))
                .stream()
                .flatMap(BufferedReader::lines)
                .collect(Collectors.toList());
        String failingPrimaryPartitionKey = extractPrimaryPartitionKeyForFailingEntries(expectedFailingEntries);

        StubS3Driver s3Driver = new StubS3Driver(bucketName, resourceFiles);
        ImportRequest request = new ImportRequest(bucketName, SOME_PATH, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver,
                                                                    mockAmazonDynamoDb(failingPrimaryPartitionKey));
        List<ImportResult> result = dataImportHandler.importAllFilesFromFolder(request);
        String errorMessages = result.stream()
                                   .map(ImportResult::getErrorMessage)
                                   .collect(Collectors.joining());

        for (String expectedFailingEntry : expectedFailingEntries) {
            assertThat(errorMessages, containsString(expectedFailingEntry));
        }
    }

    private String extractPrimaryPartitionKeyForFailingEntries(List<String> expectedFailingEntries) {
        String partitionKeyInFile = expectedFailingEntries.get(PARTITION_KEY_FOR_FAILING_ENTRIES);
        Pattern pattern = Pattern.compile(PRIMARY_KEY_LOCATOR);
        Matcher matcher = pattern.matcher(partitionKeyInFile);
        return matcher.results().map(match -> match.group(1)).findFirst().orElseThrow();
    }

    private StubS3Driver failingS3Driver(AtomicReference<String> failingContent) {
        return new StubS3Driver(bucketName, resourceFiles) {
            @Override
            public String getFile(String filename) {
                String content = super.getFile(filename);
                if (FAILING_TO_READ_FILE.equals(filename)) {
                    failingContent.set(content);
                    throw new RuntimeException();
                }
                return content;
            }
        };
    }

    private AmazonDynamoDB mockAmazonDynamoDb(final String failingPrimaryPartitionKey) {
        AmazonDynamoDB mockClient = mock(AmazonDynamoDB.class);
        when(mockClient.batchWriteItem(argThat(requestContains(failingPrimaryPartitionKey))))
            .thenAnswer(answerForFailingPrimaryPartitionKey(failingPrimaryPartitionKey));

        when(mockClient.batchWriteItem(argThat(not(requestContains(failingPrimaryPartitionKey)))))
            .thenReturn(new BatchWriteItemResult());

        return mockClient;
    }

    private Answer<BatchWriteItemResult> answerForFailingPrimaryPartitionKey(String failingPrimaryPartitionKey) {
        return new Answer<>() {
            @Override
            public BatchWriteItemResult answer(InvocationOnMock invocation) {
                BatchWriteItemRequest request = invocation.getArgument(0);
                Map<String, List<WriteRequest>> unprocessedItems = createUnprocessedValuesMapForFailingPk(request);
                return new BatchWriteItemResult().withUnprocessedItems(unprocessedItems);
            }

            private Map<String, List<WriteRequest>> createUnprocessedValuesMapForFailingPk(
                BatchWriteItemRequest request) {
                return request.getRequestItems()
                           .values()
                           .stream().flatMap(Collection::stream)
                           .filter(this::requestContainsFailingPk)
                           .map(writeRequest -> Map.of(RESOURCES_TABLE_NAME, List.of(writeRequest)))
                           .findAny()
                           .orElse(Collections.emptyMap());
            }

            private boolean requestContainsFailingPk(WriteRequest writeRequest) {
                String actualPrimaryKey = writeRequest.getPutRequest().getItem()
                                              .get(PRIMARY_KEY_PARTITION_KEY_NAME)
                                              .getS();
                return failingPrimaryPartitionKey.equals(actualPrimaryKey);
            }
        };
    }
}