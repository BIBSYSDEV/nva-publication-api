package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.PublicationGenerator.randomUri;
import static no.unit.nva.publication.s3imports.ApplicationConstants.EMPTY_STRING;
import static no.unit.nva.publication.s3imports.ApplicationConstants.ERRORS_FOLDER;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultClock;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.ERROR_REPORT_FILENAME;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.IMPORT_EVENT_TYPE_ENV_VARIABLE;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.INFORM_USER_THAT_EVENT_TYPE_IS_SET_IN_ENV;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.WRONG_OR_EMPTY_S3_LOCATION_ERROR;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public class FilenameEventEmitterTest {

    private static final String SOME_BUCKET = "someBucket";
    private static final URI SOME_S3_LOCATION = URI.create("s3://" + SOME_BUCKET + "/");

    private static final String SOME_OTHER_BUS = "someOtherBus";
    private static final Context CONTEXT = mock(Context.class);
    private static final String LIST_ALL_FILES = ".";
    private static final String PATH_SEPARATOR = "/";
    private static final Instant NOW = Instant.now();
    private static final Integer NON_ZERO_NUMBER_OF_FAILURES = 2;
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private FilenameEventEmitter handler;

    private FakeEventBridgeClient eventBridgeClient;
    private FakeS3Client s3Client;
    private Clock clock;
    private S3Driver s3Driver;
    private List<URI> injectedFiles;

    @BeforeEach
    public void init() throws IOException {
        outputStream = new ByteArrayOutputStream();
        eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME);
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, SOME_BUCKET);
        injectedFiles = List.of(insertRandomFile(), insertRandomFile(), insertRandomFile());
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient, clock);
    }

    @Test
    public void shouldThrowExceptionWhenInputDoesNotContainS3Location() throws JsonProcessingException {
        String json = invalidBody();
        Executable action = () -> handler.handleRequest(IoUtils.stringToStream(json), outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(json));
    }

    @Test
    public void handlerThrowsNoExceptionWheInputIsValidAndLocationIsNotEmpty() {
        ImportRequest importRequest = new ImportRequest(randomUri(), NOW);
        Executable action = () -> handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }

    @ParameterizedTest(
        name = "handlerEmitsEventsWithFullFileUriForEveryFilenameInS3BucketWhenInputIsAnExistingNotEmptyS3Location")
    @ValueSource(strings = {EMPTY_STRING, PATH_SEPARATOR})
    public void handlerEmitsEventsWithFullFileUriForEveryFilenameInS3BucketWhenInputIsAnExistingNotEmptyS3Location(
        String pathSeparator)
        throws IOException {

        handler = new FilenameEventEmitter(s3Client, eventBridgeClient, clock);
        var s3Location = URI.create(SOME_S3_LOCATION + pathSeparator);
        var importRequest = new ImportRequest(s3Location, NOW);
        var inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        var fileList = eventBridgeClient.listEmittedFilenames();

        assertThat(fileList, containsInAnyOrder(injectedFiles.toArray(URI[]::new)));
    }

    @Test
    public void handlerReturnsEmptyListWhenAllFilenamesHaveBeenEmittedSuccessfully()
        throws IOException {
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        PutEventsResult[] failedResultsArray =
            s3ImportsMapper.readValue(outputStream.toString(), PutEventsResult[].class);
        var failedResults = Arrays.asList(failedResultsArray);

        assertThat(failedResults, is(empty()));
    }

    @Test
    public void handlerThrowsExceptionWhenS3LocationIsNotExistentOrEmpty() {
        handler = handlerThatReceivesEmptyS3Location();

        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(),
                   containsString(WRONG_OR_EMPTY_S3_LOCATION_ERROR + importRequest.getS3Location()));
    }

    @Test
    public void handlerLogsAndReturnsFailedEventRequestsWhenEventsFailToBeEmitted() throws IOException {

        final TestAppender appender = LogUtils.getTestingAppender(FilenameEventEmitter.class);

        handler = handlerThatFailsToEmitMessages();
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);

        handler.handleRequest(inputStream, outputStream, CONTEXT);
        URI[] failedResultsArray = s3ImportsMapper.readValue(outputStream.toString(), URI[].class);
        List<URI> failedResults = Arrays.asList(failedResultsArray);

        assertThat(failedResults, containsInAnyOrder(injectedFiles.toArray(URI[]::new)));
        for (URI filename : injectedFiles) {
            assertThat(appender.getMessages(), containsString(filename.toString()));
        }
    }

    @Test
    public void handlerSavesInS3FolderErrorTimestampPathFilenameAReportContainingAllFilenamesThatFailedToBeEmitted()
        throws IOException {
        handler = handlerThatFailsToEmitMessages();
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);

        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKET);
        UnixPath errorReportFile = ERRORS_FOLDER
            .addChild(timestampToString(clock.instant()))
            .addChild(importRequest.extractPathFromS3Location())
            .addChild(ERROR_REPORT_FILENAME);
        String content = s3Driver.getFile(errorReportFile);
        for (URI filename : injectedFiles) {
            assertThat(content, containsString(filename.toString()));
        }
    }

    @Test
    public void handlerDoesNotCreateReportFileWhenNoErrorHasOccurred()
        throws IOException {

        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKET);

        List<UnixPath> allFiles = s3Driver.listAllFiles(UnixPath.of(LIST_ALL_FILES));
        assertThatBucketContainsOnlyInjectedInputFilesAndNotAnyGeneratedReportFiles(allFiles);
    }

    @Test
    public void handlerThrowsExceptionWhenEventBusCannotBeFound() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient, defaultClock());
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), containsString(ApplicationConstants.EVENT_BUS_NAME));
    }

    @Test
    public void handlerEmitsImportRequestContainingTheImportEventTypeSetInTheEnvironment()
        throws IOException {
        String expectedImportEvent = new Environment().readEnv(IMPORT_EVENT_TYPE_ENV_VARIABLE);
        ImportRequest importRequest = newImportRequest();
        handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        List<ImportRequest> emittedImportRequests = eventBridgeClient.listEmittedImportRequests();
        for (ImportRequest emittedImportRequest : emittedImportRequests) {

            assertThat(emittedImportRequest.getImportEventType(), is(equalTo(expectedImportEvent)));
        }
    }

    @Test
    public void shouldThrowExceptionSayingThatSettingImportEventTypeIsNotAllowedWhenImportEventTypeIsSetByTheUser()
        throws IOException {
        String someEventType = "someEventType";
        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION, someEventType, NOW);
        Executable action = () -> handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(INFORM_USER_THAT_EVENT_TYPE_IS_SET_IN_ENV));
    }

    @Test
    public void shouldEmmitEventIncludingATimestampBasedOnWhenItWasTriggered() throws IOException {
        Instant expectedTimestamp = clock.instant();
        ImportRequest importRequest = newImportRequest();

        handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        List<ImportRequest> emittedImportRequests = eventBridgeClient.listEmittedImportRequests();
        for (ImportRequest emittedImportRequest : emittedImportRequests) {
            assertThat(emittedImportRequest.getTimestamp(), is(equalTo(expectedTimestamp)));
        }
    }

    private URI insertRandomFile() throws IOException {
        UnixPath firstFilePath = UnixPath.of(randomString()).addChild(randomString());
        return s3Driver.insertFile(firstFilePath, randomString());
    }

    private void assertThatBucketContainsOnlyInjectedInputFilesAndNotAnyGeneratedReportFiles(List<UnixPath> allFiles) {
        var expectedFilenamesInS3Bucket = injectedFiles
            .stream()
            .map(UriWrapper::new)
            .map(UriWrapper::toS3bucketPath)
            .collect(Collectors.toList());

        assertThat(allFiles, containsInAnyOrder(expectedFilenamesInS3Bucket.toArray(UnixPath[]::new)));
    }

    private ImportRequest newImportRequest() {
        return new ImportRequest(SOME_S3_LOCATION, Instant.now());
    }

    private FilenameEventEmitter handlerThatFailsToEmitMessages() {
        EventBridgeClient eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME) {
            @Override
            protected Integer numberOfFailures() {
                return NON_ZERO_NUMBER_OF_FAILURES;
            }
        };
        return new FilenameEventEmitter(s3Client, eventBridgeClient, clock);
    }

    private FilenameEventEmitter handlerThatReceivesEmptyS3Location() {
        S3Client s3Client = new FakeS3Client(Collections.emptyMap());
        return new FilenameEventEmitter(s3Client, eventBridgeClient, defaultClock());
    }

    private <T> InputStream toJsonStream(T importRequest) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(importRequest))
            .map(IoUtils::stringToStream)
            .orElseThrow();
    }

    private String invalidBody() throws JsonProcessingException {
        ObjectNode root = s3ImportsMapper.createObjectNode();
        root.put("someField", "someValue");
        return s3ImportsMapper.writeValueAsString(root);
    }
}