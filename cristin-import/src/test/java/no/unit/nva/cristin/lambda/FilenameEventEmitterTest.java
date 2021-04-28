package no.unit.nva.cristin.lambda;

import static no.unit.nva.cristin.lambda.FilenameEventEmitter.WRONG_OR_EMPTY_S3_LOCATION_ERROR;
import static no.unit.nva.testutils.IoUtils.stringToStream;
import static nva.commons.core.JsonUtils.objectMapperWithEmpty;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;
import software.amazon.awssdk.services.s3.S3Client;

public class FilenameEventEmitterTest {

    public static final String SOME_S3_LOCATION = "s3://some/location";
    public static final String FILE_01 = "file01";
    public static final String FILE_02 = "file02";
    public static final List<String> FILE_LIST = List.of(FILE_01, FILE_02);
    public static final Map<String, InputStream> FILE_CONTENTS = fileContents();
    public static final int NON_ZERO_NUMBER_OF_FAILURES = 2;
    private static final Context CONTEXT = mock(Context.class);
    public static final String SOME_OTHER_BUS = "someOtherBus";
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private FilenameEventEmitter handler;

    private FakeEventBridgeClient eventBridgeClient;
    private FakeS3Client s3Client;

    @BeforeEach
    public void init() {
        outputStream = new ByteArrayOutputStream();
        this.eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME);

        s3Client = new FakeS3Client(FILE_CONTENTS);
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient);
    }

    @Test
    public void handlerThrowsExceptionWhenInputIsInvalid() throws JsonProcessingException {
        String json = invalidBody();
        Executable action = () -> handler.handleRequest(stringToStream(json), outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(json));
    }

    @Test
    public void handlerThrowsNoExceptionWhenInputIsValid() {
        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION);
        Executable action = () -> handler.handleRequest(toJsonStream(importRequest), outputStream, CONTEXT);
        assertDoesNotThrow(action);
    }

    @Test
    public void handlerEmitsEvensForEveryFilenameInS3BucketWhenInputIsAnExistingNotEmptyS3Location()
        throws IOException {
        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION);
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        List<String> fileList = eventBridgeClient.listEmittedFilenames();

        assertThat(fileList, containsInAnyOrder(FILE_LIST.toArray(String[]::new)));
    }

    @Test
    public void handlerReturnsEmptyListWhenAllFilenamesHaveBeenEmittedSuccessfully()
        throws IOException {
        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION);
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

        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION);
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
        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION);
        InputStream inputStream = toJsonStream(importRequest);
        handler.handleRequest(inputStream, outputStream, CONTEXT);
        String[] failedResultsArray = objectMapperWithEmpty.readValue(outputStream.toString(), String[].class);
        List<String> failedResults = Arrays.asList(failedResultsArray);

        assertThat(failedResults, containsInAnyOrder(FILE_LIST.toArray(String[]::new)));
        for (String filename : FILE_LIST) {
            assertThat(appender.getMessages(), containsString(filename));
        }
    }

    @Test
    public void handlerThrowsExceptionWhenEventBusCannotBeFound() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = new FilenameEventEmitter(s3Client, eventBridgeClient);
        ImportRequest importRequest = new ImportRequest(SOME_S3_LOCATION);
        InputStream inputStream = toJsonStream(importRequest);
        Executable action = () -> handler.handleRequest(inputStream, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        assertThat(exception.getMessage(), containsString(ApplicationConstants.EVENT_BUS_NAME));
    }

    private static Map<String, InputStream> fileContents() {
        return FILE_LIST.stream()
                   .map(file -> new SimpleEntry<>(file, fileContent()))
                   .collect(Collectors.toMap(SimpleEntry::getKey,
                                             SimpleEntry::getValue));
    }

    private static InputStream fileContent() {
        return InputStream.nullInputStream();
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