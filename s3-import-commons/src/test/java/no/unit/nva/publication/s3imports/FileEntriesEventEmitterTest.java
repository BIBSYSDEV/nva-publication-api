package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.ERRORS_FOLDER;
import static no.unit.nva.publication.s3imports.FileEntriesEventEmitter.FILE_CONTENTS_EMISSION_EVENT_TOPIC;
import static no.unit.nva.publication.s3imports.FileEntriesEventEmitter.FILE_EXTENSION_ERROR;
import static no.unit.nva.publication.s3imports.FileImportUtils.timestampToString;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.FILENAME_EMISSION_EVENT_TOPIC;
import static no.unit.nva.publication.s3imports.FilenameEventEmitter.SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER;
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
import com.amazonaws.services.sqs.model.BatchResultErrorEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.SendMessageBatchResult;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.publication.s3imports.utils.FakeAmazonSQS;
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
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.ion.IonReader;
import software.amazon.ion.IonWriter;
import software.amazon.ion.system.IonReaderBuilder;
import software.amazon.ion.system.IonTextWriterBuilder;

class FileEntriesEventEmitterTest {

    public static final String UNEXPECTED_TOPIC = "unexpected detail type";
    private static final Context CONTEXT = Mockito.mock(Context.class);
    private static final String SOME_BUCKETNAME = "someBucketname";

    public static final String UNEXPECTED_TOPIC1 = "Unexpected topic";
    private S3Client s3Client;

    private FileEntriesEventEmitter handler;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;

    private FakeAmazonSQS amazonSQS;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        amazonSQS = new FakeAmazonSQS();
        s3Driver = new S3Driver(s3Client, "notimportant");

        handler = newHandler();
        outputStream = new ByteArrayOutputStream();
    }
    
    @Test
    void handlerSavesErrorReportOutsideInputFolderSoThatErrorReportWillNotBecomeInputInSubsequentImport()
        throws IOException {
        var amazonSqsThrowingException = amazonSQSThatThrowsException();
        handler = new FileEntriesEventEmitter(s3Client, amazonSqsThrowingException);
        var fileUri = s3Driver.insertFile(randomPath(), SampleObject.random().toJsonString());
        var inputEvent = toInputStream(createInputEventForFile(fileUri));
        handler.handleRequest(inputEvent, outputStream, CONTEXT);
        var s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        var files = s3Driver.listAllFiles(ERRORS_FOLDER);
        assertThat(files, is(not(empty())));
    }

    @Test
    void shouldSendMessageToSqsWithMessagesContainingReferencesPointingToNewEventBodyWhenInputContainsSingleJsonObject()
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
    void shouldGenerateSqsMessagesWithBodyContainingAnEntryOfTheInputFileAndReferenceToTheInputFile()
        throws IOException {
        var fileContents = SampleObject.random();
        var fileToBeRead = s3Driver.insertFile(randomPath(), fileContents.toJsonString());
        var input = toInputStream(createInputEventForFile(fileToBeRead));
        var handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        var bodyOfEmittedEvent = amazonSQS.getMessageBodies().stream()
                                     .map(EventReference::fromJson)
                                     .map(EventReference::getUri)
                                     .map(eventBodyUri -> s3Driver.readEvent(eventBodyUri))
                                     .map(json -> FileContentsEvent.fromJson(json, SampleObject.class))
                                     .collect(SingletonCollector.collect());
        assertThat(bodyOfEmittedEvent.getFileUri(), is(equalTo(fileToBeRead)));
    }

    @Test
    void shouldSendMessagePointingToNewEventBodiesWhenInputContainsManySingleIndependentJsonObjects()
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
    void shouldSendSqsMessagePointingToFilesWithSingleResourcesWhenFileUriExistsAndContainsDataAsJsonArray()
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
    void shouldSendMessagesPointingToEventBodiesWhenFileUriExistsAndContainsDataAsIndependentIonObjects()
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
    void shouldSendMessagesPointingToEventBodiesWhenFileUriExistsAndContainsDataAsIonArray()
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
    void shouldSendMessageToCorrectQueue() throws IOException {
        var sampleObject = SampleObject.random();
        var fileUri = s3Driver.insertFile(randomPath(), sampleObject.toJsonString());
        var input = toInputStream(createInputEventForFile(fileUri));
        var handler = newHandler();

        handler.handleRequest(input, outputStream, CONTEXT);
        var emitedEventTopics = emittedEvents(amazonSQS)
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
        var invalidEventReference = new EventReference(UNEXPECTED_TOPIC,
                                                       SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER,
                                                       fileUri);
        var invalidInputEvent = new AwsEventBridgeEvent<EventReference>();
        invalidInputEvent.setDetail(invalidEventReference);
        
        assertThat(invalidEventReference.getTopic(), is(not(equalTo(FILENAME_EMISSION_EVENT_TOPIC))));
        Executable action = () -> handler.handleRequest(toInputStream(invalidInputEvent), outputStream, CONTEXT);
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void handlerThrowsExceptionWhenInputDoesNotHaveTheExpectedSubtopic() throws IOException {
        var sampleObject = SampleObject.random();
        var fileUri = s3Driver.insertFile(randomPath(), sampleObject.toJsonString());
        var invalidEventReference = new EventReference(FILENAME_EMISSION_EVENT_TOPIC,
                                                       UNEXPECTED_TOPIC1,
                                                       fileUri);
        var invalidInputEvent = new AwsEventBridgeEvent<EventReference>();
        invalidInputEvent.setDetail(invalidEventReference);
        Executable action = () -> handler.handleRequest(toInputStream(invalidInputEvent), outputStream, CONTEXT);
        assertThrows(IllegalArgumentException.class, action);
    }

    @Test
    void shouldSendMessageWithTheSameTimestampWhichIsEqualToTimestampAcquiredByInputEvent() throws IOException {
        var sampleObject = SampleObject.random();
        var fileUri = s3Driver.insertFile(randomPath(), sampleObject.toJsonString());
        var eventReference = new EventReference(FILENAME_EMISSION_EVENT_TOPIC,
                                                SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER,
                                                fileUri);
        var inputEvent = new AwsEventBridgeEvent<EventReference>();
        inputEvent.setDetail(eventReference);

        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var actualTimeStamp = collectTimestampFromEmittedObjects();
        assertThat(actualTimeStamp, is(equalTo(eventReference.getTimestamp())));
    }
    
    @Test
    void handlerSavesErrorReportInS3WithPathImitatingTheInputPath() throws IOException {
        var amazonSqsThrowingException = amazonSQSThatThrowsException();
        handler = new FileEntriesEventEmitter(s3Client, amazonSqsThrowingException);
        var filePath = randomPath();
        var fileUri = s3Driver.insertFile(filePath, SampleObject.random().toJsonString());
        var inputEvent = createInputEventForFile(fileUri);

        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var filename = filePath.getLastPathElement();
        var folderContainingInputFilename = filePath.getParent().orElseThrow();
        var errorFileFolder = ERRORS_FOLDER
                                  .addChild(timestampToString(inputEvent.getDetail().getTimestamp()))
                                  .addChild(folderContainingInputFilename);
        var expectedErrorFileLocation = errorFileFolder
                                            .addChild(filename + FILE_EXTENSION_ERROR)
                                            .toString();
        var s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        assertDoesNotThrow(() -> s3Driver.getFile(UnixPath.of(expectedErrorFileLocation)));
    }
    
    @Test
    void handlerThrowsExceptionWhenInputUriIsNotAnExistingFile() {
        var nonExistingFile = randomUri();
        var input = toInputStream(createInputEventForFile(nonExistingFile));
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(nonExistingFile.toString()));
    }
    
    @Test
    void shouldSaveErrorReportForNonExistingFileInPathImitatingTheInputFileUri() {
        var nonExistingFile = UriWrapper.fromUri(randomUri()).addChild(randomString()).getUri();
        var filename = UriWrapper.fromUri(nonExistingFile).getLastPathElement();
        var parent = UriWrapper.fromUri(nonExistingFile).getParent().map(UriWrapper::getPath).orElseThrow();
        var inputEvent = createInputEventForFile(nonExistingFile);
        Executable action = () -> handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var exception = assertThrows(RuntimeException.class, action);
        var expectedErrorFileLocation = ERRORS_FOLDER
                                            .addChild(timestampToString(inputEvent.getDetail().getTimestamp()))
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
        var amazonSqsThrowingException =  amazonSQSThatFailsToSendMessages();
        handler = new FileEntriesEventEmitter(s3Client, amazonSqsThrowingException);
        var contents = SampleObject.random();
        var filePath = randomPath();
        var fileUri = s3Driver.insertFile(filePath, contents.toJsonString());
        var inputEvent = createInputEventForFile(fileUri);
        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);

        var expectedErrorFileLocation = ERRORS_FOLDER
                                            .addChild(timestampToString(inputEvent.getDetail().getTimestamp()))
                                            .addChild(filePath.getParent().orElseThrow())
                                            .addChild(filePath.getLastPathElement() + FILE_EXTENSION_ERROR)
                                            .toString();

        var actualErrorFile = s3Driver.getFile(UnixPath.of(expectedErrorFileLocation));
        var putResult = parseOutPut(outputStream);
        assertThat(actualErrorFile, containsString(putResult.getFailures().get(0).toJsonString()));
    }
    
    @Test
    void shouldSaveErrorReportImitatingInputFilePathWhenFailsToEmitTheWholeFileContents() throws IOException {
        amazonSQS = amazonSQSThatThrowsException();
        handler = new FileEntriesEventEmitter(s3Client, amazonSQS);
        var filPath = randomPath();
        var fileUri = s3Driver.insertFile(filPath, SampleObject.random().toJsonString());

        var inputEvent = createInputEventForFile(fileUri);
        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);

        var expectedErrorFileLocation = ERRORS_FOLDER
                                            .addChild(timestampToString(inputEvent.getDetail().getTimestamp()))
                                            .addChild(filPath.getParent().orElseThrow())
                                            .addChild(filPath.getLastPathElement() + FILE_EXTENSION_ERROR)
                                            .toString();
        var s3Driver = new S3Driver(s3Client, SOME_BUCKETNAME);
        assertDoesNotThrow(() -> s3Driver.getFile(UnixPath.of(expectedErrorFileLocation)));
    }
    
    @Test
    void shouldOrganizeErrorReportsByTheTimeImportStarted() throws IOException {
        amazonSQS = amazonSQSThatThrowsException();
        handler = new FileEntriesEventEmitter(s3Client, amazonSQS);
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
    void shouldSendMessageWithSubtopicEqualToInputImportRequestSubtopic() throws IOException {
        var sampleEntry = SampleObject.random().toJsonString();
        var fileUri = s3Driver.insertFile(randomPath(), sampleEntry);
        var inputEvent = createInputEventForFile(fileUri);
        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var actualSubtopic = amazonSQS.getMessageBodies().stream()
                                 .map(EventReference::fromJson)
                                 .map(EventReference::getSubtopic)
                                 .collect(SingletonCollector.collect());

        assertThat(actualSubtopic, is(equalTo(inputEvent.getDetail().getSubtopic())));
    }

    @Test
    void shouldSendMessagesWithLowerCasedJsonKeyNames() throws IOException {
        var input = IoUtils.stringFromResources(Path.of("bundle.txt"));
        var fileUri = s3Driver.insertFile(randomPath(), input);
        var inputEvent = createInputEventForFile(fileUri);
        handler.handleRequest(toInputStream(inputEvent), outputStream, CONTEXT);
        var contentBodyOfEmittedEvent = amazonSQS.getMessageBodies().stream()
                                            .map(EventReference::fromJson)
                                            .map(EventReference::getUri)
                                            .map(eventBodyUri -> s3Driver.readEvent(eventBodyUri))
                                            .map(json -> FileContentsEvent.fromJson(json, JsonNode.class))
                                            .map(FileContentsEvent::getContents)
                                            .collect(Collectors.toList());
        assertThatContentBodyOfEmittedEventsFieldNamesAreAllLowerCase(contentBodyOfEmittedEvent);
    }

    private static PutSqsMessageResult parseOutPut(ByteArrayOutputStream outputStream) {
        var output = outputStream.toString(StandardCharsets.UTF_8);
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(output, PutSqsMessageResult.class)).orElseThrow();
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

    private void assertThatContentBodyOfEmittedEventsFieldNamesAreAllLowerCase(List<JsonNode> contents) {
        contents.forEach(this::assertThatFieldNamesAreLowerCase);
    }

    private void assertThatFieldNamesAreLowerCase(JsonNode content) {
        content.fieldNames().forEachRemaining(name -> assertThat(name, is(equalTo(name.toLowerCase(Locale.ROOT)))));
        content.elements().forEachRemaining(this::assertThatFieldNamesAreLowerCase);
    }

    private UnixPath randomPath() {
        return UnixPath.of(randomString(), randomString());
    }

    private List<SampleObject> collectBodiesOfEmittedEventReferences() {
        var s3Driver = new S3Driver(s3Client, "ignored");
        return amazonSQS.getMessageBodies().stream()
                   .map(EventReference::fromJson)
                   .map(EventReference::getUri)
                   .map(s3Driver::readEvent)
                   .map(json -> FileContentsEvent.fromJson(json, SampleObject.class))
                   .map(FileContentsEvent::getContents)
                   .collect(Collectors.toList());
    }

    private FakeAmazonSQS amazonSQSThatThrowsException() {
        return new FakeAmazonSQS() {
            @Override
            public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {

                throw new UnsupportedOperationException("Total failure");
            }
        };
    }

    private FakeAmazonSQS amazonSQSThatFailsToSendMessages() {
        return new FakeAmazonSQS() {
            @Override
            public SendMessageBatchResult sendMessageBatch(SendMessageBatchRequest sendMessageBatchRequest) {
                var result = new SendMessageBatchResult();
                result.setFailed(
                    sendMessageBatchRequest.getEntries().stream().map(entry -> createFailedResult(entry)).collect(
                        Collectors.toList()));
                result.setSuccessful(List.of());
                return result;
            }
        };
    }

    private BatchResultErrorEntry createFailedResult(SendMessageBatchRequestEntry entry) {
        var resultEntry = new BatchResultErrorEntry();
        resultEntry.setId(entry.getId());
        resultEntry.setMessage("Failed miserably");
        return resultEntry;
    }

    private FileEntriesEventEmitter newHandler() {
        return new FileEntriesEventEmitter(s3Client, amazonSQS);
    }

    private AwsEventBridgeEvent<EventReference> createInputEventForFile(URI fileUri) {
        var eventReference = new EventReference(FILENAME_EMISSION_EVENT_TOPIC,
                                                SUBTOPIC_SEND_EVENT_TO_FILE_ENTRIES_EVENT_EMITTER,
                                                fileUri,
                                                Instant.now());
        var request = new AwsEventBridgeEvent<EventReference>();

        request.setDetail(eventReference);
        return request;
    }

    //
    private Stream<EventReference> emittedEvents(
        FakeAmazonSQS fakeAmazonSQS) {
        return fakeAmazonSQS.getMessageBodies().stream()
                   .map(EventReference::fromJson);
    }

    private Instant collectTimestampFromEmittedObjects() {
        return emittedEvents(amazonSQS)
                   .map(EventReference::getTimestamp)
                   .collect(SingletonCollector.collect());
    }

    private InputStream toInputStream(AwsEventBridgeEvent<EventReference> request) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(request))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }
}