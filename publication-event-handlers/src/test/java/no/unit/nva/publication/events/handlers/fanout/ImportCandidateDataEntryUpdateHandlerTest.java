package no.unit.nva.publication.events.handlers.fanout;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate.IMPORT_CANDIDATE_DELETION;
import static no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate.IMPORT_CANDIDATE_UPDATE;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.AWS_REGION;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.ImportStatus;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.IdentifierEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

public class ImportCandidateDataEntryUpdateHandlerTest {

    /*
    Kun én ting som skal implementeres: at det kommer en oppdatering på en ImportPublication.
    1. lage eventen som det skal lyttes på
    2. avgjøre at eventen har rett type.
    3. avgjøre at eventen skal videre til persistence.

    Hvis ikke emit zero event.
     */

    private final Context context = Mockito.mock(Context.class);
    private OutputStream outputStream;
    private S3Driver s3Driver;
    private ImportCandidateDataEntryUpdateHandler handler;

    @BeforeEach
    void init() {
        outputStream = new ByteArrayOutputStream();
        var s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        handler = new ImportCandidateDataEntryUpdateHandler();
    }

    @ParameterizedTest(name = "should convert a DynamoDbStream to a DataEntryUpdate event")
    @MethodSource("dynamoDbUpdateEventProvider")
    void shouldEmitUpdateEventReferenceWhenSuppliedAnDynamoDbUpdateStream(ImportCandidate importCandidate,
                                                                          DynamodbStreamRecord dynamoRecord)
        throws IOException {
        var event = emulateEventSentByDynamoDbStreamToEventBridgeHandler(dynamoRecord);
        handler.handleRequest(event, outputStream, context);
        var response = parseResponse();
        assertThat(response.getTopic(), is(equalTo(IMPORT_CANDIDATE_UPDATE)));
        var blobUri = response.getUri();
        var blob = s3Driver.getFile(UriWrapper.fromUri(blobUri).toS3bucketPath());
        var eventBody = dtoObjectMapper.readValue(blob, ImportCandidateDataEntryUpdate.class);
        assertThat(eventBody.getNewData(), is(equalTo(importCandidate)));
    }

    @Test
    void shouldEmitDeletionEventReferenceWhenSuppliedADynamoDbRemoveStream() throws IOException {
        var sampleImportCandidate = generateRandomImportCandidate();
        var removeDynamoRecord = sampleDynamoRecord(sampleImportCandidate, null, OperationType.REMOVE);
        var event = emulateEventSentByDynamoDbStreamToEventBridgeHandler(removeDynamoRecord);
        handler.handleRequest(event, outputStream, context);
        var response = parseResponse();
        assertThat(response.getTopic(), is(equalTo(IMPORT_CANDIDATE_DELETION)));
        var blobUri = response.getUri();
        var blob = s3Driver.getFile(UriWrapper.fromUri(blobUri).toS3bucketPath());
        var eventBody = dtoObjectMapper.readValue(blob, ImportCandidateDataEntryUpdate.class);
        assertThat(eventBody.getOldData(), is(equalTo(sampleImportCandidate)));
    }

    @ParameterizedTest
    @MethodSource("notDaoProvider")
    void shouldNotThrowExceptionWhenEntryIsNotDao(Map<String, AttributeValue> notDao) throws IOException {
        var blob = sampleDynamoRecord(notDao, notDao);
        try (var event = emulateEventSentByDynamoDbStreamToEventBridgeHandler(blob)) {
            assertDoesNotThrow(() -> handler.handleRequest(event, outputStream, context));
            var response = parseResponse();
            assertThat(response, is(nullValue()));
        }
    }

    private static Map<String, AttributeValue> identifierEntry()
        throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var resource = Resource.fromPublication(publication);
        var identifierEntry = IdentifierEntry.create(resource.toDao());
        return convertToAttributeValueMap(identifierEntry);
    }

    private static Stream<Map<String, AttributeValue>> notDaoProvider() throws JsonProcessingException {
        return Stream.of(identifierEntry(), randomDynamoEntry());
    }

    private static Map<String, AttributeValue> randomDynamoEntry() {
        return Map.of(randomString(), new AttributeValue(randomString()));
    }

    private static Stream<Arguments> dynamoDbUpdateEventProvider() throws JsonProcessingException {
        var sampleImportCandidate = generateRandomImportCandidate();
        return
            Stream.of(Arguments.of(sampleImportCandidate,
                                   sampleDynamoRecord(null, sampleImportCandidate, OperationType.INSERT)),
                      Arguments.of(sampleImportCandidate,
                                   sampleDynamoRecord(sampleImportCandidate, sampleImportCandidate,
                                                      OperationType.MODIFY)));
    }

    private static ImportCandidate generateRandomImportCandidate() {
        var randomPublication = PublicationGenerator.randomPublication();
        return new ImportCandidate.Builder()
                   .withPublication(randomPublication)
                   .withImportStatus(ImportStatus.NOT_IMPORTED)
                   .build();
    }

    private static DynamodbEvent.DynamodbStreamRecord sampleDynamoRecord(Map<String, AttributeValue> oldImage,
                                                                         Map<String, AttributeValue> newImage) {
        return createDynamoRecord(createPayload(oldImage, newImage), OperationType.MODIFY);
    }

    private static DynamodbEvent.DynamodbStreamRecord sampleDynamoRecord(ImportCandidate oldImage,
                                                                         ImportCandidate newImage,
                                                                         OperationType operationType)
        throws JsonProcessingException {
        return createDynamoRecord(createPayload(oldImage, newImage), operationType);
    }

    private static DynamodbStreamRecord createDynamoRecord(StreamRecord payload, OperationType operationType) {
        var record = new DynamodbStreamRecord();
        record.setEventName(operationType);
        record.setEventID(randomString());
        record.setAwsRegion(AWS_REGION);

        record.setDynamodb(payload);
        record.setEventSource(randomString());
        record.setEventVersion(randomString());
        return record;
    }

    private static StreamRecord createPayload(ImportCandidate oldImage, ImportCandidate newImage)
        throws JsonProcessingException {
        return createPayload(convertToAttributeValueMap(toDynamoEntry(oldImage)),
                             convertToAttributeValueMap(toDynamoEntry(newImage)));
    }

    private static StreamRecord createPayload(Map<String, AttributeValue> oldImage,
                                              Map<String, AttributeValue> newImage) {
        var record = new StreamRecord();
        record.setOldImage(oldImage);
        record.setNewImage(newImage);
        return record;
    }

    private static Map<String, AttributeValue> convertToAttributeValueMap(DynamoEntry payload)
        throws JsonProcessingException {
        return nonNull(payload) ? toAttributeValueMap(payload) : null;
    }

    private static Map<String, AttributeValue> toAttributeValueMap(DynamoEntry dynamoEntry)
        throws JsonProcessingException {
        Map<String, com.amazonaws.services.dynamodbv2.model.AttributeValue> dynamoFormat = dynamoEntry.toDynamoFormat();
        var json = dtoObjectMapper.writeValueAsString(dynamoFormat);
        return dtoObjectMapper.readValue(json, dynamoMapStructureAsJacksonType());
    }

    private static JavaType dynamoMapStructureAsJacksonType() {
        return dtoObjectMapper.getTypeFactory().constructParametricType(Map.class, String.class, AttributeValue.class);
    }

    private static DynamoEntry toDynamoEntry(ImportCandidate importCandidate) {
        return nonNull(importCandidate) ? new ResourceDao(Resource.fromImportCandidate(importCandidate)) : null;
    }

    private EventReference parseResponse() {
        return attempt(() -> objectMapper.readValue(outputStream.toString(), EventReference.class))
                   .orElseThrow();
    }

    private InputStream emulateEventSentByDynamoDbStreamToEventBridgeHandler(DynamodbStreamRecord dynamoRecord)
        throws IOException {
        var blobUri = saveBlobInS3(dynamoRecord);
        var eventBody = new EventReference("ImportCandidate.Database.Update", blobUri);
        return EventBridgeEventBuilder.sampleEvent(eventBody);
    }

    private URI saveBlobInS3(DynamodbStreamRecord blob) throws IOException {
        var json = attempt(() -> dtoObjectMapper.writeValueAsString(blob)).orElseThrow();
        return s3Driver.insertFile(UnixPath.of(UUID.randomUUID().toString()), json);
    }
}
