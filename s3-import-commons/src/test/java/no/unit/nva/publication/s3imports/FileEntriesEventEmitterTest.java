package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static nva.commons.core.JsonUtils.objectMapperNoEmpty;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

public class FileEntriesEventEmitterTest {

    public static final String UNEXPECTED_DETAIL_TYPE = "unexpected detail type";

    public static final String SOME_USER = randomString();
    public static final String IMPORT_EVENT_TYPE = "importEventType";
    public static final ImportRequest IMPORT_REQUEST_FOR_EXISTING_FILE =
        new ImportRequest("s3://bucket/parent/folder/location.file", SOME_USER, IMPORT_EVENT_TYPE);
    public static final ImportRequest IMPORT_REQUEST_FOR_NON_EXISTING_FILE =
        new ImportRequest("s3://bucket/parent/folder/nonexisting.file", SOME_USER, IMPORT_EVENT_TYPE);
    public static final String LINE_SEPARATOR = System.lineSeparator();
    public static final SampleObject[] FILE_01_CONTENTS = randomObjects().toArray(SampleObject[]::new);
    public static final Context CONTEXT = Mockito.mock(Context.class);
    public static final String SOME_OTHER_BUS = "someOtherBus";
    public static final String SOME_BUCKETNAME = "someBucketname";
    private static final Integer NON_ZER0_NUMBER_OF_FAILURES = 2;
    public static final String ALL_FILES = ".";
    private S3Client s3Client;
    private FakeEventBridgeClient eventBridgeClient;
    private FileEntriesEventEmitter handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client(fileWithContentsAsJsonArray().toMap());
        eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME);
        handler = newHandler();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handlerEmitsEventWithResourceWhenFileUriExistsAndContainsDataAsJsonArray() {
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_EXISTING_FILE);
        FileEntriesEventEmitter handler = newHandler();

        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> emittedResourceObjects = collectEmittedObjects(eventBridgeClient);

        assertThat(emittedResourceObjects, containsInAnyOrder(FILE_01_CONTENTS));
    }

    @Test
    public void handlerThrowsExceptionWhenInputDoesNotHaveTheExpectedDetailType() {
        AwsEventBridgeEvent<ImportRequest> request = new AwsEventBridgeEvent<>();
        request.setDetailType(UNEXPECTED_DETAIL_TYPE);
        request.setDetail(IMPORT_REQUEST_FOR_EXISTING_FILE);
        InputStream input = toInputStream(request);

        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(request.getDetailType()));
    }

    @Test
    public void handlerEmitsEventWithResourceWhenFileUriExistsAndContainsDataAsJsonObjectsList() {
        s3Client = new FakeS3Client(fileWithContentsAsJsonObjectsLists().toMap());
        handler = newHandler();
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_EXISTING_FILE);

        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> emittedResourceObjects = collectEmittedObjects(eventBridgeClient);
        assertThat(emittedResourceObjects, containsInAnyOrder(FILE_01_CONTENTS));
    }

    @Test
    public void handlerThrowsExceptionWhenTryingToEmitToNonExistingEventBus() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = newHandler();
        var input = createRequestEventForFile(IMPORT_REQUEST_FOR_EXISTING_FILE);

        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        String eventBusNameUsedByHandler = ApplicationConstants.EVENT_BUS_NAME;
        assertThat(exception.getMessage(), containsString(eventBusNameUsedByHandler));
    }

    @Test
    public void handlerSavesInS3FileWhenTryingToEmitToNonExistingEventBus() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = newHandler();
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_EXISTING_FILE);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        String errorReport = extractActualErrorReport(IMPORT_REQUEST_FOR_EXISTING_FILE);
        assertThat(errorReport, is(containsString(exception.getMessage())));
    }

    @Test
    public void handlerThrowsExceptionWhenInputUriIsNotAnExistingFile() {
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_NON_EXISTING_FILE);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(IMPORT_REQUEST_FOR_NON_EXISTING_FILE.getS3Location()));
    }

    @Test
    public void handlerSavesInS3FileWhenTInputUriInNotAnExistingFile() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_NON_EXISTING_FILE);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        String errorReport = extractActualErrorReport(IMPORT_REQUEST_FOR_NON_EXISTING_FILE);
        assertThat(errorReport, is(containsString(exception.getMessage())));
    }

    @Test
    public void handlerSavesInS3FileWhenEventsFailToBeEmitted() {
        eventBridgeClient = eventBridgeClientThatFailsToEmitMessages();
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_EXISTING_FILE);
        handler.handleRequest(input, outputStream, CONTEXT);
        String errorReport = extractActualErrorReport(IMPORT_REQUEST_FOR_EXISTING_FILE);
        List<String> samplesOfExpectedContentsInReportFile = Arrays.stream(FILE_01_CONTENTS)
                                                                 .map(SampleObject::getId)
                                                                 .map(Object::toString)
                                                                 .collect(Collectors.toList());
        for (String sample : samplesOfExpectedContentsInReportFile) {
            assertThat(errorReport, containsString(sample));
        }
    }

    @Test
    public void handlerDoesNotCrateFileInS3FolderWhenNoErrorsOccur() {
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_EXISTING_FILE);
        FileEntriesEventEmitter handler = newHandler();

        handler.handleRequest(input, outputStream, CONTEXT);
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        List<String> allFiles = s3Driver.listFiles(Path.of(ALL_FILES));
        String expectedFile = IMPORT_REQUEST_FOR_EXISTING_FILE.extractPathFromS3Location();

        assertThat(allFiles, containsInAnyOrder(expectedFile));
    }

    private String extractActualErrorReport(ImportRequest importRequestForExistingFile) {
        S3Driver s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        UriWrapper expectedErrorReportFilename = expectedErrorReportUri(importRequestForExistingFile);
        return s3Driver.getFile(expectedErrorReportFilename.toS3bucketPath().toString());
    }

    @Test
    public void handlerEmitsEventsWithImportRequestsThatIncludeInputPublicationsOwner() {
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_EXISTING_FILE);
        handler.handleRequest(input, outputStream, CONTEXT);
        List<String> publicationOwners = extractPublicationOwnersFromGeneratedEvents();
        assertThat(publicationOwners.size(), is(equalTo(FILE_01_CONTENTS.length)));
        for (String publicationOwner : publicationOwners) {
            assertThat(publicationOwner, is(equalTo(SOME_USER)));
        }
    }

    @Test
    public void handlerEmitsEventsWithDetailTypeEqualToInputsImportRequestEventType() {
        String expectedImportRequestEventType = randomString();
        ImportRequest importRequestWithCustomType =
            newImportRequest(expectedImportRequestEventType);
        InputStream input = createRequestEventForFile(importRequestWithCustomType);
        handler.handleRequest(input, outputStream, CONTEXT);
        var detailTypes = extractDetailTypesFromEvents();

        assertThat(detailTypes.size(), is(equalTo(FILE_01_CONTENTS.length)));
        for (String detailType : detailTypes) {
            assertThat(detailType, is(equalTo(expectedImportRequestEventType)));
        }
    }

    @ParameterizedTest
    @MethodSource("ionContentProvider")
    public void handlerEmitsEventsWithJsonFormatWhenInputIsFileWithIonContent(
        Function<Collection<SampleObject>, FileContent> ionContentProvider) {
        List<SampleObject> sampleObjects = randomObjects();
        s3Client = new FakeS3Client(ionContentProvider.apply(sampleObjects).toMap());
        InputStream input = createRequestEventForFile(IMPORT_REQUEST_FOR_EXISTING_FILE);
        handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> emittedObjects = collectEmittedObjects(eventBridgeClient);
        assertThat(emittedObjects, containsInAnyOrder(sampleObjects.toArray(SampleObject[]::new)));
    }

    private static ImportRequest newImportRequest(String customImportRequestEventType) {
        return new ImportRequest(IMPORT_REQUEST_FOR_EXISTING_FILE.extractPathFromS3Location(),
                                 IMPORT_REQUEST_FOR_EXISTING_FILE.getPublicationsOwner(),
                                 customImportRequestEventType);
    }

    //used in parametrized test
    private static Stream<Function<Collection<SampleObject>, FileContent>> ionContentProvider() {
        return Stream.of(
            FileEntriesEventEmitterTest::fileWithContentAsIonObjectsList,
            FileEntriesEventEmitterTest::fileWithContentAsIonArray

        );
    }

    private static FileContent fileWithContentsAsJsonObjectsLists() {
        return new FileContent(IMPORT_REQUEST_FOR_EXISTING_FILE.extractPathFromS3Location(),
                               contentsAsJsonObjectsList());
    }

    private static FileContent fileWithContentsAsJsonArray() {
        return new FileContent(IMPORT_REQUEST_FOR_EXISTING_FILE.extractPathFromS3Location(), contentsAsJsonArray());
    }

    private static FileContent fileWithContentAsIonObjectsList(Collection<SampleObject> sampleObjects) {
        String ionObjectsList = createNewIonObjectsList(sampleObjects);
        //verify that this is not a list of json objects.
        assertThrows(Exception.class, () -> objectMapperNoEmpty.readTree(ionObjectsList));
        return new FileContent(IMPORT_REQUEST_FOR_EXISTING_FILE.extractPathFromS3Location(),
                               IoUtils.stringToStream(ionObjectsList));
    }

    private static FileContent fileWithContentAsIonArray(Collection<SampleObject> sampleObjects) {
        String ionArray = attempt(() -> createNewIonArray(sampleObjects)).orElseThrow();
        return new FileContent(IMPORT_REQUEST_FOR_EXISTING_FILE.extractPathFromS3Location(),
                               IoUtils.stringToStream(ionArray));
    }

    private static InputStream contentsAsJsonArray() {
        List<JsonNode> nodes = contentAsJsonNodes();
        ArrayNode root = JsonUtils.objectMapperNoEmpty.createArrayNode();
        root.addAll(nodes);
        String jsonArrayString = attempt(() -> objectMapperNoEmpty.writeValueAsString(root)).orElseThrow();
        return IoUtils.stringToStream(jsonArrayString);
    }

    private static InputStream contentsAsJsonObjectsList() {
        ObjectMapper objectMapperWithoutLineBreaks =
            objectMapperNoEmpty.configure(SerializationFeature.INDENT_OUTPUT, false);
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
                   .map(attempt(objectMapperNoEmpty::readTree))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }

    private static String createNewIonObjectsList(Collection<SampleObject> sampleObjects) {
        return sampleObjects.stream()
                   .map(attempt(objectMapperNoEmpty::writeValueAsString))
                   .map(attempt -> attempt.map(FileEntriesEventEmitterTest::jsonToIon))
                   .map(Try::orElseThrow)
                   .collect(Collectors.joining(System.lineSeparator()));
    }

    private static String createNewIonArray(Collection<SampleObject> sampleObjects) throws IOException {
        String jsonString = objectMapperNoEmpty.writeValueAsString(sampleObjects);
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

    private UriWrapper expectedErrorReportUri(ImportRequest importRequest) {
        return Try.of(importRequest)
                   .map(ImportRequest::getS3Location)
                   .map(UriWrapper::new)
                   .map(this::generateUriForErrorReportFile)
                   .orElseThrow();
    }

    private UriWrapper generateUriForErrorReportFile(UriWrapper uri) {
        Path path = uri.getPath();
        String filename = path.getFileName().toString();
        Path errorReportPath = FileEntriesEventEmitter.errorReportPath(filename);
        return uri.getParent().orElseThrow().addChild((errorReportPath));
    }

    private List<String> extractDetailTypesFromEvents() {
        return eventBridgeClient
                   .getEvenRequests()
                   .stream()
                   .flatMap(eventRequest -> eventRequest.entries().stream())
                   .map(PutEventsRequestEntry::detailType)
                   .collect(Collectors.toList());
    }

    private FileEntriesEventEmitter newHandler() {
        return new FileEntriesEventEmitter(s3Client, eventBridgeClient);
    }

    private List<String> extractPublicationOwnersFromGeneratedEvents() {
        return eventBridgeClient.getEvenRequests().stream()
                   .flatMap(eventsRequest -> eventsRequest.entries().stream())
                   .map(PutEventsRequestEntry::detail)
                   .map(attempt(detailString -> objectMapperNoEmpty.readValue(detailString, FileContentsEvent.class)))
                   .map(Try::orElseThrow)
                   .map(FileContentsEvent::getPublicationsOwner)
                   .collect(Collectors.toList());
    }

    private InputStream createRequestEventForFile(ImportRequest detail) {
        AwsEventBridgeEvent<ImportRequest> request = new AwsEventBridgeEvent<>();
        request.setDetailType(FilenameEventEmitter.EVENT_DETAIL_TYPE);
        request.setDetail(detail);
        return toInputStream(request);
    }

    private List<SampleObject> collectEmittedObjects(FakeEventBridgeClient eventBridgeClient) {
        return eventBridgeClient.getEvenRequests()
                   .stream()
                   .flatMap(e -> e.entries().stream())
                   .map(PutEventsRequestEntry::detail)
                   .map(detail -> FileContentsEvent.fromJson(detail, SampleObject.class))
                   .map(FileContentsEvent::getContents)
                   .collect(Collectors.toList());
    }

    private InputStream toInputStream(AwsEventBridgeEvent<ImportRequest> request) {
        return attempt(() -> objectMapperNoEmpty.writeValueAsString(request))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }
}