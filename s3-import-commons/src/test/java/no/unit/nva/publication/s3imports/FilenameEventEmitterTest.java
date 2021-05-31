package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static no.unit.nva.publication.s3imports.ApplicationConstants.EMPTY_STRING;
import static no.unit.nva.publication.s3imports.ApplicationConstants.ERRORS_FOLDER;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.ERROR_REPORT_FILENAME;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.PATH_SEPARATOR;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.WRONG_OR_EMPTY_S3_LOCATION_ERROR;
import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
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
import java.nio.file.Path;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
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

    public static final String SOME_BUCKET = "s3://bucket/";
    public static final String SOME_FOLDER = "some/folder/";
    public static final String SOME_S3_LOCATION = SOME_BUCKET;
    public static final String FILE_DIRECTLY_UNDER_S3_LOCATION = SOME_FOLDER + "file01";
    public static final String FILE_IN_SUBFOLDER = SOME_FOLDER + "someOtherFolder/file02";
    public static final List<String> INPUT_FILE_LIST = List.of(FILE_DIRECTLY_UNDER_S3_LOCATION, FILE_IN_SUBFOLDER);
    public static final Map<String, InputStream> FILE_CONTENTS = fileContents();
    public static final int NON_ZERO_NUMBER_OF_FAILURES = 2;
    public static final String SOME_OTHER_BUS = "someOtherBus";
    public static final String SOME_USER = randomString();
    public static final String SOME_IMPORT_EVENT_TYPE = "someImportEventType";
    private static final Context CONTEXT = mock(Context.class);
    public static final String LIST_ALL_FILES = ".";
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private FilenameEventEmitter handler;

    private FakeEventBridgeClient eventBridgeClient;
    private FakeS3Client s3Client;

    @BeforeEach
    public void init() {
        outputStream = new ByteArrayOutputStream();
        eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME);

        s3Client = new FakeS3Client(FILE_CONTENTS);
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient);
    }

    @Test
    public void handlerThrowsExceptionWhenInputIsInvalid() throws JsonProcessingException {
        String json = invalidBody();
        Executable action = () -> handler.handleRequest(IoUtils.stringToStream(json), outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(json));
    }

    @Test
    public void handlerThrowsNoExceptionWhenInputIsValid() {
        ImportRequest importRequest = newImportRequest();
        Executable action = () -> handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }

    @ParameterizedTest(
        name = "handlerEmitsEventsWithFullFileUriForEveryFilenameInS3BucketWhenInputIsAnExistingNotEmptyS3Location")
    @ValueSource(strings = {EMPTY_STRING, PATH_SEPARATOR})
    public void handlerEmitsEventsWithFullFileUriForEveryFilenameInS3BucketWhenInputIsAnExistingNotEmptyS3Location(
        String pathSeparator)
        throws IOException {
        init(); // @BeforeEach seems to not run between subsequent iterations of Parameterized test
        ImportRequest importRequest = new ImportRequest(
            SOME_S3_LOCATION + pathSeparator,
            SOME_USER,
            SOME_IMPORT_EVENT_TYPE);
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        List<String> fileList = eventBridgeClient.listEmittedFilenames();

        String[] expectedFiles = expectedFileUris();
        assertThat(fileList, containsInAnyOrder(expectedFiles));
    }

    @Test
    public void handlerReturnsEmptyListWhenAllFilenamesHaveBeenEmittedSuccessfully()
        throws IOException {
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        PutEventsResult[] failedResultsArray =
            objectMapperWithEmpty.readValue(outputStream.toString(), PutEventsResult[].class);
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

        TestAppender appender = LogUtils.getTestingAppender(FilenameEventEmitter.class);
        handler = handlerThatFailsToEmitMessages();
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        String[] failedResultsArray = objectMapperWithEmpty.readValue(outputStream.toString(), String[].class);
        List<String> failedResults = Arrays.asList(failedResultsArray);

        String[] expectedFileUris = expectedFileUris();
        assertThat(failedResults, containsInAnyOrder(expectedFileUris));
        for (String filename : INPUT_FILE_LIST) {
            assertThat(appender.getMessages(), containsString(filename));
        }
    }

    @Test
    public void handlerSavesInS3FolderErrorReportContainingAllFilenamesThatFailedToBeEmitted() throws IOException {
        handler = handlerThatFailsToEmitMessages();
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);

        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKET);
        Path errorReportFile = Path.of(ERRORS_FOLDER, importRequest.extractPathFromS3Location(), ERROR_REPORT_FILENAME);
        String content = s3Driver.getFile(errorReportFile.toString());
        for (String filename : INPUT_FILE_LIST) {
            assertThat(content, containsString(filename));
        }
    }

    @Test
    public void handlerDoesNotCreateReportFileWhenNoErrorHasOccurred()
        throws IOException {
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKET);

        List<String> allFiles = s3Driver.listFiles(Path.of(LIST_ALL_FILES));
        assertThat(allFiles, containsInAnyOrder(INPUT_FILE_LIST.toArray(String[]::new)));
    }

    @Test
    public void handlerThrowsExceptionWhenEventBusCannotBeFound() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient);
        ImportRequest importRequest = newImportRequest();
        InputStream inputStream = toJsonStream(importRequest);
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), containsString(ApplicationConstants.EVENT_BUS_NAME));
    }

    @Test
    public void handlerEmitsEventsWithThatIncludeInputPublicationsOwner() throws IOException {
        ImportRequest importRequest = newImportRequest();
        handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        List<ImportRequest> emittedImportRequests = eventBridgeClient.listEmittedImportRequests();
        for (ImportRequest emittedRequest : emittedImportRequests) {
            assertThat(emittedRequest.getPublicationsOwner(), is(equalTo(SOME_USER)));
        }
    }

    @Test
    public void handlerEmitsImportRequestContainingTheImportEventTypeContainedInTheInputImportRequest()
        throws IOException {

        String expectedImportEvent = "expectedImportEvent";
        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION, SOME_USER, expectedImportEvent);

        handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        List<ImportRequest> emittedImportRequests = eventBridgeClient.listEmittedImportRequests();
        for (ImportRequest emitedImportRequest : emittedImportRequests) {
            assertThat(emitedImportRequest.getImportEventType(), is(equalTo(expectedImportEvent)));
        }
    }

    private static Map<String, InputStream> fileContents() {
        return INPUT_FILE_LIST.stream()
                   .map(file -> new SimpleEntry<>(file, fileContent()))
                   .collect(Collectors.toMap(SimpleEntry::getKey,
                                             SimpleEntry::getValue));
    }

    private static InputStream fileContent() {
        return InputStream.nullInputStream();
    }

    private ImportRequest newImportRequest() {
        return new ImportRequest(SOME_S3_LOCATION, SOME_USER, SOME_IMPORT_EVENT_TYPE);
    }

    private String[] expectedFileUris() {
        return INPUT_FILE_LIST.stream()
                   .map(filename -> SOME_BUCKET + filename)
                   .collect(Collectors.toList())
                   .toArray(String[]::new);
    }

    private FilenameEventEmitter handlerThatFailsToEmitMessages() {
        EventBridgeClient eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME) {
            @Override
            protected Integer numberOfFailures() {
                return NON_ZERO_NUMBER_OF_FAILURES;
            }
        };
        return new FilenameEventEmitter(s3Client, eventBridgeClient);
    }

    private FilenameEventEmitter handlerThatReceivesEmptyS3Location() {
        S3Client s3Client = new FakeS3Client(Collections.emptyMap());
        return new FilenameEventEmitter(s3Client, eventBridgeClient);
    }

    private <T> InputStream toJsonStream(T importRequest) {
        return attempt(() -> objectMapperWithEmpty.writeValueAsString(importRequest))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }

    private String invalidBody() throws JsonProcessingException {
        ObjectNode root = objectMapperWithEmpty.createObjectNode();
        root.put("someField", "someValue");
        return objectMapperWithEmpty.writeValueAsString(root);
    }
}