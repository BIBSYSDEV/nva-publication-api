package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.ERRORS_FOLDER;
import static no.unit.nva.publication.s3imports.ApplicationConstants.EVENT_BUS_NAME;
import static no.unit.nva.publication.s3imports.ApplicationConstants.defaultClock;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.ERROR_REPORT_FILENAME;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.FILENAME_EMISSION_EVENT_TOPIC;
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
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

class FilenameEventEmitterTest {
    
    public static final String EMPTY_SUBTOPIC = null;
    private static final String SOME_BUCKET = "someBucket";
    private static final URI SOME_S3_LOCATION = URI.create("s3://" + SOME_BUCKET + "/");
    private static final Context CONTEXT = mock(Context.class);
    private static final String LIST_ALL_FILES = ".";
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
        eventBridgeClient = new FakeEventBridgeClient(EVENT_BUS_NAME);
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, SOME_BUCKET);
        injectedFiles = List.of(insertRandomFile(), insertRandomFile(), insertRandomFile());
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient, clock);
    }
    
    @Test
    void shouldThrowExceptionWhenInputDoesNotContainS3Location() throws IOException {
        String json = invalidBody();
        Executable action = () -> handler.handleRequest(IoUtils.stringToStream(json), outputStream, CONTEXT);
        var exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(json));
    }
    
    @Test
    void shouldNotThrowExceptionWheInputIsValidAndLocationIsNotEmpty() {
        var importRequest = newImportRequest();
        Executable action = () -> handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }
    
    @ParameterizedTest(
        name = "shouldEmitEventsWithFullFileUriForEveryFilenameInS3BucketWhenInputIsAnExistingNotEmptyS3Location")
    @ValueSource(strings = {"", "/"})
    void shouldEmitEventsWithFullFileUriForEveryFilenameInS3BucketWhenInputIsAnExistingNotEmptyS3Location(
        String pathSeparator)
        throws IOException {
        
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient, clock);
        var s3Location = URI.create(SOME_S3_LOCATION + pathSeparator);
        var importRequest = new EventReference(FILENAME_EMISSION_EVENT_TOPIC, EMPTY_SUBTOPIC, s3Location, NOW);
        var inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        var fileList = fetchEmittedEventReferences()
                           .stream()
                           .map(EventReference::getUri)
                           .collect(Collectors.toList());
        
        assertThat(fileList, containsInAnyOrder(injectedFiles.toArray(URI[]::new)));
    }
    
    @Test
    void handlerReturnsEmptyListWhenAllFilenamesHaveBeenEmittedSuccessfully()
        throws IOException {
        var importRequest = newImportRequest();
        var inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        PutEventsResult[] failedResultsArray =
            s3ImportsMapper.readValue(outputStream.toString(), PutEventsResult[].class);
        var failedResults = Arrays.asList(failedResultsArray);
        
        assertThat(failedResults, is(empty()));
    }
    
    @Test
    void handlerThrowsExceptionWhenS3LocationIsNotExistentOrEmpty() {
        handler = handlerThatReceivesEmptyS3Location();
        
        EventReference importRequest = newImportRequest();
        var inputStream = toJsonStream(importRequest);
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(),
            containsString(WRONG_OR_EMPTY_S3_LOCATION_ERROR + importRequest.getUri()));
    }
    
    @Test
    void handlerLogsAndReturnsFailedEventRequestsWhenEventsFailToBeEmitted() throws IOException {
        
        final TestAppender appender = LogUtils.getTestingAppender(FilenameEventEmitter.class);
        
        handler = handlerThatFailsToEmitMessages();
        EventReference importRequest = newImportRequest();
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
    void handlerSavesInS3FolderErrorTimestampPathFilenameAReportContainingAllFilenamesThatFailedToBeEmitted()
        throws IOException {
        handler = handlerThatFailsToEmitMessages();
        EventReference importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKET);
        UnixPath errorReportFile = ERRORS_FOLDER
                                       .addChild(timestampToString(clock.instant()))
                                       .addChild(extractPathFromS3Location(importRequest))
                                       .addChild(ERROR_REPORT_FILENAME);
        String content = s3Driver.getFile(errorReportFile);
        for (URI filename : injectedFiles) {
            assertThat(content, containsString(filename.toString()));
        }
    }
    
    private UnixPath extractPathFromS3Location(EventReference importRequest) {
        return UriWrapper.fromUri(importRequest.getUri()).toS3bucketPath();
    }
    
    @Test
    void handlerDoesNotCreateReportFileWhenNoErrorHasOccurred()
        throws IOException {
        
        EventReference importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKET);
        
        List<UnixPath> allFiles = s3Driver.listAllFiles(UnixPath.of(LIST_ALL_FILES));
        assertThatBucketContainsOnlyInjectedInputFilesAndNotAnyGeneratedReportFiles(allFiles);
    }
    
    @Test
    void handlerThrowsExceptionWhenEventBusCannotBeFound() {
        eventBridgeClient = new FakeEventBridgeClient();
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient, defaultClock());
        EventReference importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), containsString(EVENT_BUS_NAME));
    }
    
    @Test
    void shouldEmmitEventWithEventTriggerTimestamp() throws IOException {
        Instant expectedTimestamp = clock.instant();
        EventReference importRequest = newImportRequest();
    
        handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        List<EventReference> emittedImportRequests = fetchEmittedEventReferences();
        for (EventReference emittedImportRequest : emittedImportRequests) {
            assertThat(emittedImportRequest.getTimestamp(), is(equalTo(expectedTimestamp)));
        }
    }
    
    private List<EventReference> fetchEmittedEventReferences() {
        return eventBridgeClient.getRequestEntries()
                   .stream()
                   .map(PutEventsRequestEntry::detail)
                   .map(EventReference::fromJson)
                   .collect(Collectors.toList());
    }
    
    @Test
    void shouldEmitEventWithTopicFilenameEmissionEvent() throws IOException {
        EventReference importRequest = newImportRequest();
        handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        var actualEmittedEvents = fetchEmittedEventReferences();
        
        for (var emittedEvent : actualEmittedEvents) {
            assertThat(emittedEvent.getTopic(),
                is(equalTo(FilenameEventEmitter.FILENAME_EMISSION_EVENT_TOPIC)));
            assertThat(emittedEvent.getSubtopic(),
                is(equalTo(FilenameEventEmitter.FILENAME_EMISSION_EVENT_SUBTOPIC)));
        }
    }
    
    private URI insertRandomFile() throws IOException {
        UnixPath firstFilePath = UnixPath.of(randomString()).addChild(randomString());
        return s3Driver.insertFile(firstFilePath, randomString());
    }
    
    private void assertThatBucketContainsOnlyInjectedInputFilesAndNotAnyGeneratedReportFiles(List<UnixPath> allFiles) {
        var expectedFilenamesInS3Bucket = injectedFiles
                                              .stream()
                                              .map(UriWrapper::fromUri)
                                              .map(UriWrapper::toS3bucketPath)
                                              .collect(Collectors.toList());
        
        assertThat(allFiles, containsInAnyOrder(expectedFilenamesInS3Bucket.toArray(UnixPath[]::new)));
    }
    
    private EventReference newImportRequest() {
        return new EventReference(FILENAME_EMISSION_EVENT_TOPIC,
            EMPTY_SUBTOPIC,
            SOME_S3_LOCATION,
            NOW);
    }
    
    private FilenameEventEmitter handlerThatFailsToEmitMessages() {
        var eventBridgeClient = new FakeEventBridgeClient(NON_ZERO_NUMBER_OF_FAILURES, EVENT_BUS_NAME) {
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