package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.ERRORS_FOLDER;
import static no.unit.nva.publication.s3imports.FileEntriesEventEmitter.FILE_CONTENTS_EMISSION_EVENT_TOPIC;
import static no.unit.nva.publication.s3imports.FileEntriesEventEmitter.FILE_EXTENSION_ERROR;
import static no.unit.nva.publication.s3imports.FileEntriesEventEmitter.PARTIAL_FAILURE;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.FILENAME_EMISSION_EVENT_SUBTOPIC;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.FILENAME_EMISSION_EVENT_TOPIC;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

public class FileEntriesEventEmitterTest {

    public static final String UNEXPECTED_TOPIC = "unexpected detail type";

    private static final URI S3_BUCKET = URI.create("s3://bucket");
    private static final String INPUT_PATH = "/parent/folder";
    private static final String INPUT_FILENAME = "location.file";
    private static final URI INPUT_URI =
        UriWrapper.fromUri(S3_BUCKET).addChild(INPUT_PATH).addChild(INPUT_FILENAME).getUri();
    private static final String NON_EXISTING_FILE = "nonexistingfile.txt";
    private static final URI NON_EXISTING_FILE_URI =
        UriWrapper.fromUri(S3_BUCKET).addChild(INPUT_PATH).addChild(NON_EXISTING_FILE).getUri();
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final SampleObject[] FILE_01_CONTENTS = randomObjects().toArray(SampleObject[]::new);
    private static final Context CONTEXT = Mockito.mock(Context.class);
    private static final String SOME_OTHER_BUS = "someOtherBus";
    private static final String SOME_BUCKETNAME = "someBucketname";
    private static final String ALL_FILES = ".";
    private static final Integer NON_ZER0_NUMBER_OF_FAILURES = 2;
    public static final String TOPIC_EMITTED_BY_FILENAME_EMITTER = FILENAME_EMISSION_EVENT_TOPIC;
    private static ImportRequest importRequestForExistingFile;
    private static ImportRequest importRequestForNonExistingFile;
    private S3Client s3Client;
    private FakeEventBridgeClient eventBridgeClient;
    private FileEntriesEventEmitter handler;
    private ByteArrayOutputStream outputStream;
    private Instant timestamp;

    @BeforeEach
    public void init() {
        timestamp = Instant.now();
        importRequestForExistingFile = new ImportRequest(TOPIC_EMITTED_BY_FILENAME_EMITTER,
                                                         FILENAME_EMISSION_EVENT_SUBTOPIC,
                                                         INPUT_URI,
                                                         timestamp);
        importRequestForNonExistingFile = new ImportRequest(TOPIC_EMITTED_BY_FILENAME_EMITTER,
                                                            FILENAME_EMISSION_EVENT_SUBTOPIC,
                                                            NON_EXISTING_FILE_URI,
                                                            timestamp);
        s3Client = new FakeS3Client(fileWithContentsAsJsonArray().toMap());
        eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME);
        handler = newHandler();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void shouldEmitEventWithResourceWhenFileUriExistsAndContainsDataAsJsonArray() {
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        FileEntriesEventEmitter handler = newHandler();

        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> emittedResourceObjects = collectEmittedObjects(eventBridgeClient);

        assertThat(emittedResourceObjects, containsInAnyOrder(FILE_01_CONTENTS));
    }

    @Test
    public void shouldEmitEventWithTopicEqualToDataEntryEmissionTopic() {
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        FileEntriesEventEmitter handler = newHandler();

        handler.handleRequest(input, outputStream, CONTEXT);
        var emitedEventTopics = emitedEvents(eventBridgeClient)
            .map(FileContentsEvent::getTopic)
            .collect(Collectors.toSet());
        assertThat(emitedEventTopics, hasSize(1));
        var actualEmittedTopic = emitedEventTopics.stream().collect(SingletonCollector.collect());
        assertThat(actualEmittedTopic, is(equalTo(FILE_CONTENTS_EMISSION_EVENT_TOPIC)));
    }

    @Test
    public void shouldAcceptEventsWithTopicEqualToFilenameEmissionTopic() {
        InputStream validEvent = createRequestEventForFile(importRequestForExistingFile);
        assertThat(importRequestForExistingFile.getTopic(), is(equalTo(FILENAME_EMISSION_EVENT_TOPIC)));
        assertDoesNotThrow(() -> handler.handleRequest(validEvent, outputStream, CONTEXT));

        ImportRequest invalidRequest = importRequestForExistingFile.withTopic(randomString());
        InputStream invalidEvent = createRequestEventForFile(invalidRequest);
        assertThat(invalidRequest.getTopic(), is(not(equalTo(FILENAME_EMISSION_EVENT_TOPIC))));
        assertThrows(IllegalArgumentException.class, () -> handler.handleRequest(invalidEvent, outputStream, CONTEXT));
    }

    @Test
    public void handlerThrowsExceptionWhenInputDoesNotHaveTheExpectedTopic() {
        AwsEventBridgeEvent<ImportRequest> request = new AwsEventBridgeEvent<>();
        ImportRequest invalidRequest = importRequestForExistingFile.withTopic(UNEXPECTED_TOPIC);
        request.setDetail(invalidRequest);
        InputStream input = toInputStream(request);

        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(invalidRequest.getTopic()));
    }

    @Test
    public void handlerEmitsEventWithResourceWhenFileUriExistsAndContainsDataAsJsonObjectsList() {
        s3Client = new FakeS3Client(fileWithContentsAsJsonObjectsLists().toMap());
        handler = newHandler();
        InputStream input = createRequestEventForFile(importRequestForExistingFile);

        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> emittedResourceObjects = collectEmittedObjects(eventBridgeClient);
        assertThat(emittedResourceObjects, containsInAnyOrder(FILE_01_CONTENTS));
    }

    @Test
    public void handlerEmitsEventWithTimestampAcquiredByImportRequest() {
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        handler.handleRequest(input, outputStream, CONTEXT);
        List<Instant> emittedResourceObjects = collectTimestampFromEmittedObjects(eventBridgeClient);
        for (Instant actuallTimestamp : emittedResourceObjects) {
            assertThat(actuallTimestamp, is(equalTo(timestamp)));
        }
    }

    @Test
    public void handlerThrowsExceptionWhenTryingToEmitToNonExistingEventBus() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = newHandler();
        var input = createRequestEventForFile(importRequestForExistingFile);

        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        String eventBusNameUsedByHandler = ApplicationConstants.EVENT_BUS_NAME;
        assertThat(exception.getMessage(), containsString(eventBusNameUsedByHandler));
    }

    @Test
    public void handlerSavesInS3FileWhenTryingToEmitToNonExistingEventBus() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = newHandler();
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        String expectedErrorFileLocation = ERRORS_FOLDER
            .addChild(timestampToString(timestamp))
            .addChild(exception.getClass().getSimpleName())
            .addChild(INPUT_PATH)
            .addChild(INPUT_FILENAME + FILE_EXTENSION_ERROR)
            .toString();
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        String actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        assertThat(actualErrorFile, is(containsString(exception.getMessage())));
    }

    @Test
    public void handlerThrowsExceptionWhenInputUriIsNotAnExistingFile() {
        InputStream input = createRequestEventForFile(importRequestForNonExistingFile);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(importRequestForNonExistingFile.getS3Location().toString()));
    }

    @Test
    public void handlerSavesInS3FileWhenTInputUriInNotAnExistingFile() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        InputStream input = createRequestEventForFile(importRequestForNonExistingFile);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        String expectedErrorFileLocation = ERRORS_FOLDER
            .addChild(timestampToString(timestamp))
            .addChild(exception.getClass().getSimpleName())
            .addChild(INPUT_PATH)
            .addChild(NON_EXISTING_FILE + FILE_EXTENSION_ERROR)
            .toString();
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        String actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        assertThat(actualErrorFile, is(containsString(exception.getMessage())));
    }

    @Test
    public void handlerSavesInS3FileWhenEventsFailToBeEmitted() {
        eventBridgeClient = eventBridgeClientThatFailsToEmitMessages();
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        handler.handleRequest(input, outputStream, CONTEXT);
        String expectedErrorFileLocation =
            ERRORS_FOLDER
                .addChild(timestampToString(timestamp))
                .addChild(PARTIAL_FAILURE)
                .addChild(INPUT_PATH)
                .addChild(INPUT_FILENAME + FILE_EXTENSION_ERROR)
                .toString();

        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        String actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        List<String> samplesOfExpectedContentsInReportFile = Arrays.stream(FILE_01_CONTENTS)
            .map(SampleObject::getId)
            .map(Object::toString)
            .collect(Collectors.toList());
        for (String sample : samplesOfExpectedContentsInReportFile) {
            assertThat(actualErrorFile, containsString(sample));
        }
    }

    @Test
    public void handlerSavesErrorReportOutsideInputFolder() {
        eventBridgeClient = eventBridgeClientThatFailsToEmitMessages();
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        handler.handleRequest(input, outputStream, CONTEXT);
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        List<UnixPath> files = s3Driver.listAllFiles(ERRORS_FOLDER);
        assertThat(files, is(not(empty())));
    }

    @Test
    public void handlerSavesFileInErrorsExceptionFilePathWhenFailingToEmitAllEntries() {
        eventBridgeClient = eventBridgeClientThatFailsToEmitAllMessages();
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        Exception exception = assertThrows(RuntimeException.class, action);

        String expectedErrorFileLocation = ERRORS_FOLDER
            .addChild(timestampToString(timestamp))
            .addChild(exception.getClass().getSimpleName())
            .addChild(INPUT_PATH)
            .addChild(INPUT_FILENAME + FILE_EXTENSION_ERROR)
            .toString();
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        String actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        assertThat(actualErrorFile, containsString(INPUT_URI.toString()));
    }

    @Test
    public void handlerSavesFileInErrorsTimestampExceptionNameFilePathWhenFailingToEmitSomeEntries() {
        eventBridgeClient = eventBridgeClientThatFailsToEmitMessages();
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        handler.handleRequest(input, outputStream, CONTEXT);

        String expectedErrorFileLocation = ERRORS_FOLDER
            .addChild(timestampToString(timestamp))
            .addChild(PARTIAL_FAILURE)
            .addChild(INPUT_PATH)
            .addChild(INPUT_FILENAME + FILE_EXTENSION_ERROR)
            .toString();
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        String actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        List<String> samplesOfExpectedContentsInReportFile = Arrays.stream(FILE_01_CONTENTS)
            .map(SampleObject::getId)
            .map(Object::toString)
            .collect(Collectors.toList());
        for (String sample : samplesOfExpectedContentsInReportFile) {
            assertThat(actualErrorFile, containsString(sample));
        }
    }

    @Test
    public void handlerDoesNotCrateFileInS3FolderWhenNoErrorsOccur() {
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        FileEntriesEventEmitter handler = newHandler();

        handler.handleRequest(input, outputStream, CONTEXT);
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        List<UnixPath> allFiles = s3Driver.listAllFiles(UnixPath.of(ALL_FILES));
        UnixPath expectedFile = importRequestForExistingFile.extractPathFromS3Location();

        assertThat(allFiles, containsInAnyOrder(expectedFile));
    }

    @Test
    public void handlerEmitsEventsWithSubtopicEqualToInputsImportRequestSubtopic() {
        String inputImportEventSubtopic = randomString();
        ImportRequest inputImportEvent =
            importRequestWithCustomSubtopic(inputImportEventSubtopic);
        InputStream input = createRequestEventForFile(inputImportEvent);
        handler.handleRequest(input, outputStream, CONTEXT);
        var subtopic = extractSubtopicsFromEvents();

        assertThat(subtopic.size(), is(equalTo(FILE_01_CONTENTS.length)));
        for (String detailType : subtopic) {
            assertThat(detailType, is(equalTo(inputImportEventSubtopic)));
        }
    }

    @ParameterizedTest
    @MethodSource("ionContentProvider")
    public void handlerEmitsEventsWithJsonFormatWhenInputIsFileWithIonContent(
        Function<Collection<SampleObject>, FileContent> ionContentProvider) {
        List<SampleObject> sampleObjects = randomObjects();
        var stringInputStreamMap = ionContentProvider.apply(sampleObjects).toMap();
        s3Client = new FakeS3Client(stringInputStreamMap);
        InputStream input = createRequestEventForFile(importRequestForExistingFile);
        handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> emittedObjects = collectEmittedObjects(eventBridgeClient);
        assertThat(emittedObjects, containsInAnyOrder(sampleObjects.toArray(SampleObject[]::new)));
    }

    private static ImportRequest importRequestWithCustomSubtopic(String subtopic) {
        return new ImportRequest(FILENAME_EMISSION_EVENT_TOPIC,
                                 subtopic,
                                 importRequestForExistingFile.getS3Location(),
                                 Instant.now());
    }

    //used in parametrized test
    private static Stream<Function<Collection<SampleObject>, FileContent>> ionContentProvider() {
        return Stream.of(
            FileEntriesEventEmitterTest::fileWithContentAsIonObjectsList,
            FileEntriesEventEmitterTest::fileWithContentAsIonArray

        );
    }

    private static FileContent fileWithContentsAsJsonObjectsLists() {
        return new FileContent(importRequestForExistingFile.extractPathFromS3Location(),
                               contentsAsJsonObjectsList());
    }

    private static FileContent fileWithContentsAsJsonArray() {
        return new FileContent(importRequestForExistingFile.extractPathFromS3Location(), contentsAsJsonArray());
    }

    private static FileContent fileWithContentAsIonObjectsList(Collection<SampleObject> sampleObjects) {
        String ionObjectsList = createNewIonObjectsList(sampleObjects);
        //verify that this is not a list of json objects.

        assertThrows(Exception.class, () -> s3ImportsMapper.readTree(ionObjectsList));
        return new FileContent(importRequestForExistingFile.extractPathFromS3Location(),
                               IoUtils.stringToStream(ionObjectsList));
    }

    private static FileContent fileWithContentAsIonArray(Collection<SampleObject> sampleObjects) {
        String ionArray = attempt(() -> createNewIonArray(sampleObjects)).orElseThrow();
        return new FileContent(importRequestForExistingFile.extractPathFromS3Location(),
                               IoUtils.stringToStream(ionArray));
    }

    private static InputStream contentsAsJsonArray() {
        List<JsonNode> nodes = contentAsJsonNodes();
        ArrayNode root = s3ImportsMapper.createArrayNode();
        root.addAll(nodes);
        String jsonArrayString = attempt(() -> s3ImportsMapper.writeValueAsString(root)).orElseThrow();
        return IoUtils.stringToStream(jsonArrayString);
    }

    private static InputStream contentsAsJsonObjectsList() {
        ObjectMapper objectMapperWithoutLineBreaks =
            s3ImportsMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        String nodesInLines = contentAsJsonNodes()
            .stream()
            .map(attempt(objectMapperWithoutLineBreaks::writeValueAsString))
            .map(Try::orElseThrow)
            .collect(Collectors.joining(LINE_SEPARATOR));
        return IoUtils.stringToStream(nodesInLines);
    }

    private static List<JsonNode> contentAsJsonNodes() {
        return Stream.of(FILE_01_CONTENTS)
            .map(JsonSerializable::toJsonString)
            .map(attempt(s3ImportsMapper::readTree))
            .map(Try::orElseThrow)
            .collect(Collectors.toList());
    }

    private static String createNewIonObjectsList(Collection<SampleObject> sampleObjects) {
        return sampleObjects.stream()
            .map(attempt(s3ImportsMapper::writeValueAsString))
            .map(attempt -> attempt.map(FileEntriesEventEmitterTest::jsonToIon))
            .map(Try::orElseThrow)
            .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String createNewIonArray(Collection<SampleObject> sampleObjects) throws IOException {
        String jsonString = s3ImportsMapper.writeValueAsString(sampleObjects);
        return jsonToIon(jsonString);
    }

    private static String jsonToIon(String jsonString) throws IOException {
        IonReader reader = IonReaderBuilder.standard().build(jsonString);
        StringBuilder stringAppender = new StringBuilder();
        IonWriter writer = IonTextWriterBuilder.standard().build(stringAppender);
        writer.writeValues(reader);
        return stringAppender.toString();
    }

    private static List<SampleObject> randomObjects() {
        return Stream.of(SampleObject.random(), SampleObject.random(), SampleObject.random())
            .collect(Collectors.toList());
    }

    private FakeEventBridgeClient eventBridgeClientThatFailsToEmitMessages() {
        return new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME) {
            @Override
            public Integer numberOfFailures() {
                return NON_ZER0_NUMBER_OF_FAILURES;
            }
        };
    }

    private FakeEventBridgeClient eventBridgeClientThatFailsToEmitAllMessages() {
        return new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME) {

            @Override
            public PutEventsResponse putEvents(PutEventsRequest putEventsRequest) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private List<String> extractSubtopicsFromEvents() {
        return emitedEvents(eventBridgeClient)
            .map(FileContentsEvent::getSubtopic)
            .collect(Collectors.toList());
    }

    private FileEntriesEventEmitter newHandler() {
        return new FileEntriesEventEmitter(s3Client, eventBridgeClient);
    }

    private InputStream createRequestEventForFile(ImportRequest detail) {
        AwsEventBridgeEvent<ImportRequest> request = new AwsEventBridgeEvent<>();
        request.setDetail(detail);
        return toInputStream(request);
    }

    private List<SampleObject> collectEmittedObjects(FakeEventBridgeClient eventBridgeClient) {
        return emitedEvents(eventBridgeClient)
            .map(FileContentsEvent::getContents)
            .collect(Collectors.toList());
    }

    private Stream<FileContentsEvent<SampleObject>> emitedEvents(
        FakeEventBridgeClient eventBridgeClient) {
        return eventBridgeClient.getEvenRequests()
            .stream()
            .flatMap(e -> e.entries().stream())
            .map(PutEventsRequestEntry::detail)
            .map(detail -> FileContentsEvent.fromJson(detail, SampleObject.class));
    }

    private List<Instant> collectTimestampFromEmittedObjects(FakeEventBridgeClient eventBridgeClient) {
        return emitedEvents(eventBridgeClient)
            .map(FileContentsEvent::getTimestamp)
            .collect(Collectors.toList());
    }

    private InputStream toInputStream(AwsEventBridgeEvent<ImportRequest> request) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(request))
            .map(IoUtils::stringToStream)
            .orElseThrow();
    }
}