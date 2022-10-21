package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.ERRORS_FOLDER;
import static no.unit.nva.publication.s3imports.ApplicationConstants.EVENT_BUS_NAME;
import static no.unit.nva.publication.s3imports.FileEntriesEventEmitter.FILE_CONTENTS_EMISSION_EVENT_TOPIC;
import static no.unit.nva.publication.s3imports.FileEntriesEventEmitter.FILE_EXTENSION_ERROR;
import static no.unit.nva.publication.s3imports.FileEntriesEventEmitter.PARTIAL_FAILURE;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.FILENAME_EMISSION_EVENT_TOPIC;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Try;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

class FileEntriesEventEmitterTest {
    
    public static final String UNEXPECTED_TOPIC = "unexpected detail type";
    private static final Context CONTEXT = Mockito.mock(Context.class);
    private static final String SOME_OTHER_BUS = "someOtherBus";
    private static final String SOME_BUCKETNAME = "someBucketname";
    
    private static final Integer NON_ZER0_NUMBER_OF_FAILURES = 2;
    private S3Client s3Client;
    private FakeEventBridgeClient eventBridgeClient;
    private FileEntriesEventEmitter handler;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;
    
    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "notimportant");
        eventBridgeClient = new FakeEventBridgeClient(EVENT_BUS_NAME);
        handler = newHandler();
        outputStream = new ByteArrayOutputStream();
    }
    
    @Test
    void handlerSavesErrorReportOutsideInputFolderSoThatErrorReportWillNotBecomeInputInSubsequentImport()
        throws IOException {
        eventBridgeClient = new FakeEventBridgeClient(NON_ZER0_NUMBER_OF_FAILURES, EVENT_BUS_NAME);
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        var fileUri = s3Driver.insertFile(randomPath(), SampleObject.random().toJsonString());
        var inputEvent = toInputStream(createInputEventForFile(fileUri));
        handler.handleRequest(inputEvent, outputStream, CONTEXT);
        var s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        var files = s3Driver.listAllFiles(ERRORS_FOLDER);
        assertThat(files, is(not(empty())));
    }
    
    @Test
    void shouldEmitEventReferencesPointingToNewEventBodyWhenInputContainsSingleJsonObject()
        throws IOException {
        var sampleObject = SampleObject.random();
        var fileContents = sampleObject.toJsonString();
        var fileUri = s3Driver.insertFile(randomPath(), fileContents);
        var input = toInputStream(createInputEventForFile(fileUri));
        var handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> eventBodiesOfEmittedEventReferences = collectBodiesOfEmittedEventReferences();
    
        assertThat(eventBodiesOfEmittedEventReferences, containsInAnyOrder(sampleObject));
    }
    
    @Test
    void shouldGenerateEventsWithBodyContainingAnEntryOfTheInputFileAndReferenceToTheInputFile() throws IOException {
        var fileContents = SampleObject.random();
        var fileToBeRead = s3Driver.insertFile(randomPath(), fileContents.toJsonString());
        var input = toInputStream(createInputEventForFile(fileToBeRead));
        var handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        var bodyOfEmittedEvent = eventBridgeClient.getRequestEntries()
                                     .stream()
                                     .map(PutEventsRequestEntry::detail)
                                     .map(EventReference::fromJson)
                                     .map(EventReference::getUri)
                                     .map(eventBodyUri -> s3Driver.readEvent(eventBodyUri))
                                     .map(json -> FileContentsEvent.fromJson(json, SampleObject.class))
                                     .collect(SingletonCollector.collect());
        assertThat(bodyOfEmittedEvent.getFileUri(), is(equalTo(fileToBeRead)));
    }
    
    @Test
    void shouldEmitEventReferencesPointingToNewEventBodiesWhenInputContainsManySingleIndependentJsonObjects()
        throws IOException {
        var firstObject = SampleObject.random();
        var secondObject = SampleObject.random();
        var fileContents = firstObject.toJsonString() + System.lineSeparator() + secondObject.toJsonString();
        var fileUri = s3Driver.insertFile(randomPath(), fileContents);
        var input = toInputStream(createInputEventForFile(fileUri));
        var handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> eventBodiesOfEmittedEventReferences = collectBodiesOfEmittedEventReferences();
        assertThat(eventBodiesOfEmittedEventReferences, containsInAnyOrder(firstObject, secondObject));
    }
    
    @Test
    void shouldEmitEventReferencesPointingToFilesWithSingleResourcesWhenFileUriExistsAndContainsDataAsJsonArray()
        throws IOException {
        var firstObject = SampleObject.random();
        var secondObject = SampleObject.random();
        var objectList = List.of(firstObject, secondObject);
        var fileContents = JsonUtils.dtoObjectMapper.writeValueAsString(objectList);
        var fileUri = s3Driver.insertFile(randomPath(), fileContents);
        var input = toInputStream(createInputEventForFile(fileUri));
        var handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        var eventBodiesOfEmittedEventReferences = collectBodiesOfEmittedEventReferences();
        
        assertThat(eventBodiesOfEmittedEventReferences, containsInAnyOrder(firstObject, secondObject));
    }
    
    @Test
    void shouldEmitEventReferencesPointingToEventBodiesWhenFileUriExistsAndContainsDataAsIndependentIonObjects()
        throws IOException {
        var firstObject = SampleObject.random();
        var secondObject = SampleObject.random();
        var fileContents = createNewIonObjectsList(firstObject, secondObject);
        var fileUri = s3Driver.insertFile(randomPath(), fileContents);
        var input = toInputStream(createInputEventForFile(fileUri));
        var handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        var eventBodiesOfEmittedEventReferences = collectBodiesOfEmittedEventReferences();
        
        assertThat(eventBodiesOfEmittedEventReferences, containsInAnyOrder(firstObject, secondObject));
    }
    
    @Test
    void shouldEmitEventReferencesPointingToEventBodiesWhenFileUriExistsAndContainsDataAsIonArray()
        throws IOException {
        var firstObject = SampleObject.random();
        var secondObject = SampleObject.random();
        var fileContents = createNewIonArray(firstObject, secondObject);
        var fileUri = s3Driver.insertFile(randomPath(), fileContents);
        var input = toInputStream(createInputEventForFile(fileUri));
        var handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        var eventBodiesOfEmittedEventReferences = collectBodiesOfEmittedEventReferences();
        
        assertThat(eventBodiesOfEmittedEventReferences, containsInAnyOrder(firstObject, secondObject));
    }
    
    @Test
    void shouldEmitEventWithTopicEqualToDataEntryEmissionTopic() throws IOException {
        var sampleObject = SampleObject.random();
        var fileUri = s3Driver.insertFile(randomPath(), sampleObject.toJsonString());
        var input = toInputStream(createInputEventForFile(fileUri));
        var handler = newHandler();
        
        handler.handleRequest(input, outputStream, CONTEXT);
        var emitedEventTopics = emittedEvents(eventBridgeClient)
                                    .map(EventReference::getTopic)
                                    .collect(Collectors.toSet());
        assertThat(emitedEventTopics, hasSize(1));
        var actualEmittedTopic = emitedEventTopics.stream().collect(SingletonCollector.collect());
        assertThat(actualEmittedTopic, is(equalTo(FILE_CONTENTS_EMISSION_EVENT_TOPIC)));
    }
    
    @Test
    void shouldAcceptEventsWithTopicEqualToFilenameEmissionTopic() throws IOException {
        var sampleObject = SampleObject.random();
        var fileUri = s3Driver.insertFile(randomPath(), sampleObject.toJsonString());
        var inputEvent = createInputEventForFile(fileUri);
        assertThat(inputEvent.getDetail().getTopic(), is(equalTo(FILENAME_EMISSION_EVENT_TOPIC)));
        assertDoesNotThrow(() -> handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT));
    }
    
    @Test
    void handlerThrowsExceptionWhenInputDoesNotHaveTheExpectedTopic() throws IOException {
        var sampleObject = SampleObject.random();
        var fileUri = s3Driver.insertFile(randomPath(), sampleObject.toJsonString());
        var invalidEventReference = new EventReference(UNEXPECTED_TOPIC, fileUri);
        var invalidInputEvent = new AwsEventBridgeEvent<EventReference>();
        invalidInputEvent.setDetail(invalidEventReference);
        
        assertThat(invalidEventReference.getTopic(), is(not(equalTo(FILENAME_EMISSION_EVENT_TOPIC))));
        Executable action = () -> handler.handleRequest(toInputStream(invalidInputEvent), outputStream, CONTEXT);
        assertThrows(IllegalArgumentException.class, action);
    }
    
    @Test
    void shouldEmitsEventWithTheSameTimestampWhichIsEqualToTimestampAcquiredByInputEvent() throws IOException {
        var sampleObject = SampleObject.random();
        var fileUri = s3Driver.insertFile(randomPath(), sampleObject.toJsonString());
        var eventReference = new EventReference(FILENAME_EMISSION_EVENT_TOPIC, fileUri);
        var inputEvent = new AwsEventBridgeEvent<EventReference>();
        inputEvent.setDetail(eventReference);
        
        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var actualTimeStamp = collectTimestampFromEmittedObjects(eventBridgeClient);
        assertThat(actualTimeStamp, is(equalTo(eventReference.getTimestamp())));
    }
    
    @Test
    void handlerThrowsExceptionWhenTryingToEmitToNonExistingEventBus() throws IOException {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = newHandler();
        var fileUri = s3Driver.insertFile(randomPath(), SampleObject.random().toJsonString());
        var inputEvent = toInputStream(createInputEventForFile(fileUri));
        Executable action = () -> handler.handleRequest(inputEvent, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            action);
        assertThat(exception.getMessage(), containsString(EVENT_BUS_NAME));
    }
    
    @Test
    void handlerSavesErrorReportInS3WithPathImitatingTheInputPath() throws IOException {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = newHandler();
        var filePath = randomPath();
        var fileUri = s3Driver.insertFile(filePath, SampleObject.random().toJsonString());
        var inputEvent = createInputEventForFile(fileUri);
        
        Executable action = () -> handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var exception = assertThrows(IllegalStateException.class, action);
        var filename = filePath.getLastPathElement();
        var folderContainingInputFilename = filePath.getParent().orElseThrow();
        var errorFileFolder = ERRORS_FOLDER
                                  .addChild(timestampToString(inputEvent.getDetail().getTimestamp()))
                                  .addChild(exception.getClass().getSimpleName())
                                  .addChild(folderContainingInputFilename);
        var expectedErrorFileLocation = errorFileFolder
                                            .addChild(filename + FILE_EXTENSION_ERROR)
                                            .toString();
        var s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        var actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        assertThat(actualErrorFile, is(containsString(exception.getMessage())));
    }
    
    @Test
    void handlerThrowsExceptionWhenInputUriIsNotAnExistingFile() {
        var nonExistingFile = randomUri();
        var input = toInputStream(createInputEventForFile(nonExistingFile));
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(nonExistingFile.toString()));
    }
    
    @Test
    void shouldSaveErrorReportForNonExistingFileInPathImitatingTheInputFileUri() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        var nonExistingFile = UriWrapper.fromUri(randomUri()).addChild(randomString()).getUri();
        var filename = UriWrapper.fromUri(nonExistingFile).getLastPathElement();
        var parent = UriWrapper.fromUri(nonExistingFile).getParent().map(UriWrapper::getPath).orElseThrow();
        var inputEvent = createInputEventForFile(nonExistingFile);
        Executable action = () -> handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var exception = assertThrows(IllegalArgumentException.class, action);
        var expectedErrorFileLocation = ERRORS_FOLDER
                                            .addChild(timestampToString(inputEvent.getDetail().getTimestamp()))
                                            .addChild(exception.getClass().getSimpleName())
                                            .addChild(parent)
                                            .addChild(filename + FILE_EXTENSION_ERROR)
                                            .toString();
        var s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        var actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        assertThat(actualErrorFile, is(containsString(exception.getMessage())));
    }
    
    @Test
    void shouldSaveErrorReportContainingTheEventReferenceThatFailedToBeEmitted()
        throws IOException {
        eventBridgeClient = new FakeEventBridgeClient(NON_ZER0_NUMBER_OF_FAILURES, EVENT_BUS_NAME);
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        var contents = SampleObject.random();
        var filePath = randomPath();
        var fileUri = s3Driver.insertFile(filePath, contents.toJsonString());
        var inputEvent = createInputEventForFile(fileUri);
        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        
        var expectedErrorFileLocation = ERRORS_FOLDER
                                            .addChild(timestampToString(inputEvent.getDetail().getTimestamp()))
                                            .addChild(PARTIAL_FAILURE)
                                            .addChild(filePath.getParent().orElseThrow())
                                            .addChild(filePath.getLastPathElement() + FILE_EXTENSION_ERROR)
                                            .toString();
        
        var actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        var oneOfManyFailedAttemptsToEmitTheContentsEvent =
            eventBridgeClient.getRequestEntries()
                .stream()
                .map(PutEventsRequestEntry::detail)
                .map(EventReference::fromJson)
                .findFirst()
                .orElseThrow();
        var eventBodyUri = oneOfManyFailedAttemptsToEmitTheContentsEvent.getUri();
        
        assertThat(actualErrorFile, containsString(eventBodyUri.toString()));
    }
    
    @Test
    void shouldSaveErrorReportImitatingInputFilePathWhenFailsToEmitTheWholeFileContents() throws IOException {
        eventBridgeClient = eventBridgeClientThatFailsToEmitAllMessages();
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        var filPath = randomPath();
        var fileUri = s3Driver.insertFile(filPath, SampleObject.random().toJsonString());
        
        var inputEvent = createInputEventForFile(fileUri);
        Executable action = () -> handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        
        var expectedErrorFileLocation = ERRORS_FOLDER
                                            .addChild(timestampToString(inputEvent.getDetail().getTimestamp()))
                                            .addChild(exception.getClass().getSimpleName())
                                            .addChild(filPath.getParent().orElseThrow())
                                            .addChild(filPath.getLastPathElement() + FILE_EXTENSION_ERROR)
                                            .toString();
        var s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        var actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        assertThat(actualErrorFile, containsString(fileUri.toString()));
    }
    
    @Test
    void shouldOrganizeErrorReportsByTheTimeImportStarted() throws IOException {
        eventBridgeClient = new FakeEventBridgeClient(NON_ZER0_NUMBER_OF_FAILURES, EVENT_BUS_NAME);
        handler = new FileEntriesEventEmitter(s3Client, eventBridgeClient);
        var contents = SampleObject.random();
        var filePath = randomPath();
        var fileUri = s3Driver.insertFile(filePath, contents.toJsonString());
        var inputEvent = createInputEventForFile(fileUri);
        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        
        var expectedFolderStructure = ERRORS_FOLDER
                                          .addChild(timestampToString(inputEvent.getDetail().getTimestamp()));
        var errorReports = s3Driver.listAllFiles(expectedFolderStructure);
        assertThat(errorReports, is(not(empty())));
    }
    
    @Test
    void shouldNotCreateErrorReportWhenNoErrorsOccur() throws IOException {
        var sampleContent = SampleObject.random().toJsonString();
        var fileUri = s3Driver.insertEvent(randomPath(), sampleContent);
        var event = toInputStream(createInputEventForFile(fileUri));
        handler.handleRequest(event, outputStream, CONTEXT);
        
        var errorFiles = s3Driver.listAllFiles(ERRORS_FOLDER);
        assertThat(errorFiles, is(empty()));
    }
    
    @Test
    void shouldEmitEventsWithSubtopicEqualToInputImportRequestSubtopic() throws IOException {
        var sampleEntry = SampleObject.random().toJsonString();
        var fileUri = s3Driver.insertFile(randomPath(), sampleEntry);
        var inputEvent = createInputEventForFile(fileUri);
        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var actualSubtopic = eventBridgeClient.getRequestEntries().stream()
                                 .map(PutEventsRequestEntry::detail)
                                 .map(EventReference::fromJson)
                                 .map(EventReference::getSubtopic)
                                 .collect(SingletonCollector.collect());
    
        assertThat(actualSubtopic, is(equalTo(inputEvent.getDetail().getSubtopic())));
    }
    
    private static String createNewIonObjectsList(SampleObject... sampleObjects) {
        return Arrays.stream(sampleObjects)
                   .map(attempt(s3ImportsMapper::writeValueAsString))
                   .map(attempt -> attempt.map(FileEntriesEventEmitterTest::jsonToIon))
                   .map(Try::orElseThrow)
                   .collect(Collectors.joining(System.lineSeparator()));
    }
    
    private static String createNewIonArray(SampleObject... sampleObjects) throws IOException {
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
    
    private UnixPath randomPath() {
        return UnixPath.of(randomString(), randomString());
    }
    
    private List<SampleObject> collectBodiesOfEmittedEventReferences() {
        var s3Driver = new S3Driver(s3Client, "ignored");
        return eventBridgeClient.getRequestEntries()
                   .stream()
                   .map(PutEventsRequestEntry::detail)
                   .map(EventReference::fromJson)
                   .map(EventReference::getUri)
                   .map(s3Driver::readEvent)
                   .map(json -> FileContentsEvent.fromJson(json, SampleObject.class))
                   .map(FileContentsEvent::getContents)
                   .collect(Collectors.toList());
    }
    
    private FakeEventBridgeClient eventBridgeClientThatFailsToEmitAllMessages() {
        return new FakeEventBridgeClient(EVENT_BUS_NAME) {
            
            @Override
            public PutEventsResponse putEvents(PutEventsRequest putEventsRequest) {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    private FileEntriesEventEmitter newHandler() {
        return new FileEntriesEventEmitter(s3Client, eventBridgeClient);
    }
    
    private AwsEventBridgeEvent<EventReference> createInputEventForFile(URI fileUri) {
        var eventReference = new EventReference(FILENAME_EMISSION_EVENT_TOPIC,
            randomString(),
            fileUri,
            Instant.now());
        var request = new AwsEventBridgeEvent<EventReference>();
        
        request.setDetail(eventReference);
        return request;
    }
    
    //
    private Stream<EventReference> emittedEvents(
        FakeEventBridgeClient eventBridgeClient) {
        return eventBridgeClient.getRequestEntries()
                   .stream()
                   .map(PutEventsRequestEntry::detail)
                   .map(EventReference::fromJson);
    }
    
    private Instant collectTimestampFromEmittedObjects(FakeEventBridgeClient eventBridgeClient) {
        return emittedEvents(eventBridgeClient)
                   .map(EventReference::getTimestamp)
                   .collect(SingletonCollector.collect());
    }
    
    private InputStream toInputStream(AwsEventBridgeEvent<EventReference> request) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(request))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }
}