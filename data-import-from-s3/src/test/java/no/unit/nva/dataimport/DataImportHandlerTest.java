package no.unit.nva.dataimport;

import static no.unit.nva.dataimport.BatchWriteItemRequestMatcher.requestContains;
import static no.unit.nva.dataimport.DataImportHandler.EMPTY_LIST_ERROR;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.stream.Stream;
import no.unit.nva.publication.StubS3Driver;
import no.unit.nva.publication.s3imports.ImportResult;
import no.unit.nva.publication.service.ResourcesDynamoDbLocalTest;
import nva.commons.core.JsonUtils;
import nva.commons.core.SingletonCollector;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class DataImportHandlerTest extends ResourcesDynamoDbLocalTest {

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
    private static final String S3_LOCATION = "s3://orestis-export/some/location";
    private AmazonDynamoDB dynamoDbClient;
    private List<String> resourceFiles;

    @BeforeEach
    public void init() {
        super.init();
        this.dynamoDbClient = super.client;

        this.resourceFiles = List.of(FIRST_SAMPLE, SECOND_SAMPLE, THIRD_SAMPLE, FOURTH_SAMPLE);
    }

    @Test
    @Tag("RemoteTest")
    public void dataImportReadsIonFileWithResourcesAndStoresThemInDynamoDbRemote() {

        String s3Location = "s3://orestis-export/AWSDynamoDB/01617869890675-2abaf414/data/";
        String tableName = "nva-resources-orestis-resources-nva-publication";

        ImportRequest request = new ImportRequest(s3Location, tableName);
        DataImportHandler dataImportHandler = new DataImportHandler();
        dataImportHandler.importAllFilesFromFolder(request.toMap());

        Integer itemCount = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getCount();
        assertThat(itemCount, is(greaterThan(0)));
    }

    @Test
    public void dataImportReadsIonFileWithResourcesAndStoresThemInDynamoDb() {
        StubS3Driver s3Driver = new StubS3Driver(S3_LOCATION, resourceFiles);
        ImportRequest request = new ImportRequest(S3_LOCATION, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);
        dataImportHandler.importAllFilesFromFolder(request.toMap());

        Integer itemCount = client.scan(new ScanRequest().withTableName(RESOURCES_TABLE_NAME)).getCount();
        assertThat(itemCount, is(equalTo(s3Driver.getAllIonItems().size())));
    }

    @ParameterizedTest(name = "dataImportHandler throws exception when InputRequest does not contain the expected "
                              + "fields")
    @MethodSource("invalidArgumentsProvider")
    public void dataImportHandlerThrowsExceptionWhenInputRequestDoesNotContainTheExpectedFields(
        Map<String, String> invalidRequests) {
        StubS3Driver s3Driver = new StubS3Driver(S3_LOCATION, resourceFiles);

        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);
        Executable action = () -> dataImportHandler.importAllFilesFromFolder(invalidRequests);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(),
                   either((containsString(ImportRequest.S3_LOCATION_FIELD)))
                       .or(containsString(ImportRequest.TABLE_FIELD)));
    }

    @Test
    public void dataImportReturnsAllFilenamesOfFailedInputsWhenReadingFromS3Fails() {

        AtomicReference<String> failingContent = new AtomicReference<>();

        StubS3Driver s3Driver = failingS3Driver(failingContent);
        ImportRequest request = new ImportRequest(S3_LOCATION, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);
        List<ImportResult<FailedDynamoEntriesReport>> failures =
            dataImportHandler.importAllFilesFromFolder(request.toMap());
        List<String> failedFiles = failures.stream()
                                       .map(ImportResult::getInput)
                                       .map(FailedDynamoEntriesReport::getInputFilename)
                                       .collect(Collectors.toList());

        assertThat(failedFiles, contains(FAILING_TO_READ_FILE));
    }

    @Test
    public void dataImportReturnsAllFailingPrimaryKeysOfFailedInputsForPartiallySuccessfulInsertions() {
        resourceFiles = List.of(SAMPLE_WITH_SOME_FAILING_ENTRIES);

        List<String> expectedFailingEntries =
            attempt(() -> IoUtils.inputStreamFromResources(FAILING_ENTRIES))
                .map(stream -> new BufferedReader(new InputStreamReader(stream)))
                .stream()
                .flatMap(BufferedReader::lines)
                .collect(Collectors.toList());
        String failingPrimaryPartitionKey = extractPrimaryPartitionKeyForFailingEntries(expectedFailingEntries);

        StubS3Driver s3Driver = new StubS3Driver(S3_LOCATION, resourceFiles);
        ImportRequest request = new ImportRequest(S3_LOCATION, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver,
                                                                    mockAmazonDynamoDb(failingPrimaryPartitionKey));
        List<ImportResult<FailedDynamoEntriesReport>> result =
            dataImportHandler.importAllFilesFromFolder(request.toMap());
        String errorMessages = result.stream()
                                   .map(ImportResult::getInput)
                                   .map(FailedDynamoEntriesReport::getEntryKeys)
                                   .collect(Collectors.joining());

        for (String expectedFailingEntry : expectedFailingEntries) {
            assertThat(errorMessages, containsString(expectedFailingEntry));
        }
    }

    @Test
    public void dataImportLogsImportResultWhenFailureOccurs() throws JsonProcessingException {
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        List<ImportResult<FailedDynamoEntriesReport>> failures = handlerWithInputThatCannotBeWrittenToDynamo();

        String resultJson = JsonUtils.objectMapper.writeValueAsString(failures);
        assertThat(appender.getMessages(), containsString(resultJson));
    }

    @Test
    public void dataImportReturnsAllFilenamesOfFailedInputsWhenWritingToDynamoDbFails() {
        List<ImportResult<FailedDynamoEntriesReport>> failures = handlerWithInputThatCannotBeWrittenToDynamo();

        String failingFilename = failures.stream()
                                     .map(ImportResult::getInput)
                                     .map(FailedDynamoEntriesReport::getInputFilename)
                                     .collect(SingletonCollector.collect());
        String errorMessage = failures.stream().map(ImportResult::getException).collect(Collectors.joining());

        assertThat(failingFilename, is(equalTo(FAILING_TO_WRITE_FILE)));
        assertThat(errorMessage, containsString(ERROR_DUE_TO_WRONG_KEY));
    }

    @Test
    public void importAllFilesFromFolderThrowsExceptionWhenThereAreNoFilesInTheSpecifiedBucket() {
        resourceFiles = Collections.emptyList();
        StubS3Driver s3Driver = new StubS3Driver(S3_LOCATION, resourceFiles);
        ImportRequest request = new ImportRequest(S3_LOCATION, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);

        Executable action = () -> dataImportHandler.importAllFilesFromFolder(request.toMap());
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);

        assertThat(exception.getMessage(), is(equalTo(EMPTY_LIST_ERROR)));
    }

    @Test
    public void handlerLogsInput() {
        TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        StubS3Driver s3Driver = new StubS3Driver(S3_LOCATION, resourceFiles);
        ImportRequest request = new ImportRequest(S3_LOCATION, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);

        dataImportHandler.importAllFilesFromFolder(request.toMap());
        assertThat(appender.getMessages(), containsString(request.toJsonString()));
    }

    private static Stream<Map<String, String>> invalidArgumentsProvider() {
        Map<String, String> missingS3Location = new ImportRequest(null, RESOURCES_TABLE_NAME).toMap();
        Map<String, String> missingTable = new ImportRequest(S3_LOCATION, null).toMap();
        Map<String, String> wrongFields = Map.of("someField", "someValue");
        return Stream.of(missingS3Location, missingTable, wrongFields);
    }

    private List<ImportResult<FailedDynamoEntriesReport>> handlerWithInputThatCannotBeWrittenToDynamo() {
        resourceFiles = List.of(FIRST_SAMPLE, FAILING_TO_WRITE_FILE);

        StubS3Driver s3Driver = new StubS3Driver(S3_LOCATION, resourceFiles);
        ImportRequest request = new ImportRequest(S3_LOCATION, RESOURCES_TABLE_NAME);
        DataImportHandler dataImportHandler = new DataImportHandler(s3Driver, dynamoDbClient);

        return dataImportHandler.importAllFilesFromFolder(request.toMap());
    }

    private String extractPrimaryPartitionKeyForFailingEntries(List<String> expectedFailingEntries) {
        String partitionKeyInFile = expectedFailingEntries.get(PARTITION_KEY_FOR_FAILING_ENTRIES);
        Pattern pattern = Pattern.compile(PRIMARY_KEY_LOCATOR);
        Matcher matcher = pattern.matcher(partitionKeyInFile);
        return matcher.results().map(match -> match.group(1)).findFirst().orElseThrow();
    }

    private StubS3Driver failingS3Driver(AtomicReference<String> failingContent) {
        return new StubS3Driver(S3_LOCATION, resourceFiles) {
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