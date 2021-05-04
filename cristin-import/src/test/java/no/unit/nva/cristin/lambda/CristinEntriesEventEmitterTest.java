package no.unit.nva.cristin.lambda;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static nva.commons.core.JsonUtils.objectMapperNoEmpty;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.IoUtils;
import nva.commons.core.JsonSerializable;
import nva.commons.core.JsonUtils;
import nva.commons.core.attempt.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.S3Client;

public class CristinEntriesEventEmitterTest {

    public static final String UNEXPECTED_DETAIL_TYPE = "unexpected detail type";

    public static final String SOME_USER = randomString();
    public static final ImportRequest EXISTING_FILE = new ImportRequest("s3://some/s3/location.file", SOME_USER);
    public static final ImportRequest NON_EXISTING_FILE = new ImportRequest("s3://some/s3/nonexisting.file", SOME_USER);
    public static final String LINE_SEPARATOR = System.lineSeparator();

    public static final SampleObject[] FILE_01_CONTENTS = randomContents().toArray(SampleObject[]::new);
    public static final Context CONTEXT = mock(Context.class);
    public static final String SOME_OTHER_BUS = "someOtherBus";
    private S3Client s3Client;
    private FakeEventBridgeClient eventBridgeClient;
    private CristinEntriesEventEmitter handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client(filesWithContentsAsJsonArrays());
        eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME);
        handler = newHandler();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void handlerEmitsEventWithResourceWhenFileUriExistsAndContainsDataAsJsonArray() {
        InputStream input = createRequestEventForFile(EXISTING_FILE);
        CristinEntriesEventEmitter handler = newHandler();
        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> emittedResourceObjects = collectEmittedObjects(eventBridgeClient);

        assertThat(emittedResourceObjects, containsInAnyOrder(FILE_01_CONTENTS));
    }

    @Test
    public void handlerThrowsExceptionWhenInputDoesNotHaveTheExpectedDetailType() {
        AwsEventBridgeEvent<ImportRequest> request = new AwsEventBridgeEvent<>();
        request.setDetailType(UNEXPECTED_DETAIL_TYPE);
        request.setDetail(EXISTING_FILE);
        InputStream input = toInputStream(request);

        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(request.getDetailType()));
    }

    @Test
    public void handlerEmitsEventWithResourceWhenFileUriExistsAndContainsDataAsJsonObjectsList() {
        s3Client = new FakeS3Client(filesWithContentsAsJsonObjectsLists());
        handler = newHandler();
        InputStream input = createRequestEventForFile(EXISTING_FILE);

        handler.handleRequest(input, outputStream, CONTEXT);
        List<SampleObject> emittedResourceObjects = collectEmittedObjects(eventBridgeClient);

        assertThat(emittedResourceObjects, containsInAnyOrder(FILE_01_CONTENTS));
    }

    @Test
    public void handlerThrowsExceptionWhenTryingToEmitToNonExistingEventBus() {
        eventBridgeClient = new FakeEventBridgeClient(SOME_OTHER_BUS);
        handler = newHandler();
        var input = createRequestEventForFile(EXISTING_FILE);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalStateException exception = assertThrows(IllegalStateException.class, action);
        String eventBusNameUsedByHandler = ApplicationConstants.EVENT_BUS_NAME;
        assertThat(exception.getMessage(), containsString(eventBusNameUsedByHandler));
    }

    @Test
    public void handlerThrowsExceptionWhenInputUriIsNotAnExistingFile() {
        InputStream input = createRequestEventForFile(NON_EXISTING_FILE);
        Executable action = () -> handler.handleRequest(input, outputStream, CONTEXT);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action);
        assertThat(exception.getMessage(), containsString(NON_EXISTING_FILE.getS3Location()));
    }

    @Test
    public void handlerEmitsEventsWithImportRequestsThatIncludeInputPublicationsOwner() {
        InputStream input = createRequestEventForFile(EXISTING_FILE);
        handler.handleRequest(input, outputStream, CONTEXT);
        List<String> publicationOwners = extractPublicationOwnersFromGeneratedEvents();
        assertThat(publicationOwners.size(), is(equalTo(FILE_01_CONTENTS.length)));
        for (String publicationOwner : publicationOwners) {
            assertThat(publicationOwner, is(equalTo(SOME_USER)));
        }
    }

    private static Map<String, InputStream> filesWithContentsAsJsonObjectsLists() {
        return Map.of(EXISTING_FILE.extractPathFromS3Location(), contentsAsJsonObjectsList());
    }

    private static Map<String, InputStream> filesWithContentsAsJsonArrays() {
        return Map.of(EXISTING_FILE.extractPathFromS3Location(), contentsAsJsonArray());
    }

    private static List<SampleObject> randomContents() {
        return Stream.of(SampleObject.random(), SampleObject.random(), SampleObject.random())
                   .collect(Collectors.toList());
    }

    private static InputStream contentsAsJsonArray() {
        List<JsonNode> nodes = contentAsJsonNodes();
        ArrayNode root = JsonUtils.objectMapperNoEmpty.createArrayNode();
        root.addAll(nodes);
        String jsonArrayString = attempt(() -> objectMapperNoEmpty.writeValueAsString(root)).orElseThrow();
        return IoUtils.stringToStream(jsonArrayString);
    }

    private static InputStream contentsAsJsonObjectsList() {
        var objectMapper = objectMapperNoEmpty.configure(SerializationFeature.INDENT_OUTPUT, false);
        String nodesInLines = contentAsJsonNodes()
                                  .stream()
                                  .map(attempt(objectMapper::writeValueAsString))
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

    private CristinEntriesEventEmitter newHandler() {
        return new CristinEntriesEventEmitter(s3Client, eventBridgeClient);
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
        request.setDetailType(CristinFilenameEventEmitter.EVENT_DETAIL_TYPE);
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