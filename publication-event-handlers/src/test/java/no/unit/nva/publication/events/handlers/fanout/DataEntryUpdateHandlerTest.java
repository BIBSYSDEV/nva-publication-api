package no.unit.nva.publication.events.handlers.fanout;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.AWS_REGION;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.dynamodbstream.DynamodbStreamToEventBridgeHandler.DYNAMODB_UPDATE_EVENT_TOPIC;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
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
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.daos.DynamoEntry;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

public class DataEntryUpdateHandlerTest {

    private OutputStream outputStream;
    private Context context;
    private DataEntryUpdateHandler handler;
    private S3Driver s3Driver;

    public static Stream<SampleTestInput> dynamoDbEventProvider() throws JsonProcessingException {
        var samplePublication = createRandomPublicationWithoutLegacyDoiRequestEntry();
        return
            Stream.of(new SampleTestInput(samplePublication, sampleDynamoRecord(samplePublication, null)),
                      new SampleTestInput(samplePublication, sampleDynamoRecord(null, samplePublication)),
                      new SampleTestInput(samplePublication, sampleDynamoRecord(samplePublication, samplePublication)));
    }

    public static Stream<Map<String, AttributeValue>> notDaoProvider() throws JsonProcessingException {
        return Stream.of(identifierEntry(), randomDynamoEntry());
    }

    @BeforeEach
    public void setUp() {
        outputStream = new ByteArrayOutputStream();
        context = Mockito.mock(Context.class);
        var s3Client = new FakeS3Client();
        handler = new DataEntryUpdateHandler(s3Client);
        s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
    }

    @ParameterizedTest(name = "should return a DataEntryUpdateEvent when event contains a DynamoDbStreamRecord")
    @MethodSource("dynamoDbEventProvider")
    void shouldReadEventBlobFromUriInEventReference(SampleTestInput testInput) throws IOException {
        var publication = testInput.getPublication();

        var blob = testInput.getDynamoRecord();
        var blobUri = saveBlobInS3(blob);
        var eventBody = new EventReference(DYNAMODB_UPDATE_EVENT_TOPIC, blobUri);
        var event = EventBridgeEventBuilder.sampleEvent(eventBody);
        handler.handleRequest(event, outputStream, context);
        var output = parseResponse();
        var actualDataEntry = nonNull(output.getNewData()) ? output.getNewData() : output.getOldData();
        assertThat(actualDataEntry.getIdentifier(), is(equalTo(publication.getIdentifier())));
    }

    @ParameterizedTest
    @MethodSource("notDaoProvider")
    void shouldNotThrowExceptionWhenEntryIsNotDao(Map<String, AttributeValue> notDao) throws IOException {
        var blob = sampleDynamoRecord(notDao, notDao);
        var blobUri = saveBlobInS3(blob);
        var eventBody = new EventReference(DYNAMODB_UPDATE_EVENT_TOPIC, blobUri);
        var event = EventBridgeEventBuilder.sampleEvent(eventBody);

        assertDoesNotThrow(() -> handler.handleRequest(event, outputStream, context));
        var response = parseResponse();
        assertThat(response.getNewData(), is(nullValue()));
        assertThat(response.getOldData(), is(nullValue()));
    }

    private static Map<String, AttributeValue> randomDynamoEntry() {
        return Map.of(randomString(), new AttributeValue(randomString()));
    }

    private static Publication createRandomPublicationWithoutLegacyDoiRequestEntry() {
        var samplePublication = PublicationGenerator.randomPublication();
        samplePublication.setDoiRequest(null);
        return samplePublication;
    }

    private static DynamodbEvent.DynamodbStreamRecord sampleDynamoRecord(Publication oldImage, Publication newImage)
        throws JsonProcessingException {
        return createDynamoRecord(createPayload(oldImage, newImage));
    }

    private static DynamodbEvent.DynamodbStreamRecord sampleDynamoRecord(Map<String, AttributeValue> oldImage,
                                                                         Map<String, AttributeValue> newImage) {
        return createDynamoRecord(createPayload(oldImage, newImage));
    }

    private static DynamodbStreamRecord createDynamoRecord(StreamRecord payload) {
        var record = new DynamodbStreamRecord();
        record.setEventName(randomElement(OperationType.values()));
        record.setEventID(randomString());
        record.setAwsRegion(AWS_REGION);

        record.setDynamodb(payload);
        record.setEventSource(randomString());
        record.setEventVersion(randomString());
        return record;
    }

    private static StreamRecord createPayload(Publication oldImage, Publication newImage)
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

    private static DynamoEntry toDynamoEntry(Publication publication) {
        return nonNull(publication) ? new ResourceDao(Resource.fromPublication(publication)) : null;
    }

    private static <T> Map<String, AttributeValue> convertToAttributeValueMap(DynamoEntry payload)
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

    private static Map<String, AttributeValue> identifierEntry()
        throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        var resource = Resource.fromPublication(publication);
        var identifierEntry = IdentifierEntry.create(resource);
        return convertToAttributeValueMap(identifierEntry);
    }

    private URI saveBlobInS3(DynamodbStreamRecord blob) throws IOException {
        var json = attempt(() -> dtoObjectMapper.writeValueAsString(blob)).orElseThrow();
        return s3Driver.insertFile(UnixPath.of(UUID.randomUUID().toString()), json);
    }

    private InputStream randomEventBridgeEvent(Publication oldImage, Publication newImage)
        throws JsonProcessingException {
        return EventBridgeEventBuilder.sampleEvent(sampleDynamoRecord(oldImage, newImage));
    }

    private DataEntryUpdateEvent parseResponse() {
        return attempt(() -> objectMapper.readValue(outputStream.toString(), DataEntryUpdateEvent.class))
            .orElseThrow();
    }

    private static class SampleTestInput {

        private final Publication publication;
        private final DynamodbStreamRecord dynamoRecord;

        private SampleTestInput(Publication publication,
                                DynamodbStreamRecord dynamoRecord) {
            this.publication = publication;
            this.dynamoRecord = dynamoRecord;
        }

        public Publication getPublication() {
            return publication;
        }

        public DynamodbStreamRecord getDynamoRecord() {
            return dynamoRecord;
        }
    }
}
