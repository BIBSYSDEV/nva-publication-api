package no.unit.nva.publication.events.handlers.log;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.AWS_REGION;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.DynamoEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PersistLogEntryEventHandlerTest extends ResourcesLocalTest {

    private PersistLogEntryEventHandler handler;
    private ResourceService resourceService;
    private FakeS3Client s3Client;
    private ByteArrayOutputStream outputStream;
    private Context context;

    @BeforeEach
    public void setUp() {
        super.init();
        outputStream = new ByteArrayOutputStream();
        context = null;
        s3Client = new FakeS3Client();
        resourceService = getResourceServiceBuilder().build();
        handler = new PersistLogEntryEventHandler(s3Client, resourceService);
    }

    @Test
    void shouldCreateLogEntryWhenConsumedEventHasResourceWithNewImageWhereResourceEventIsPresent()
        throws BadRequestException, IOException {
        var publication = createPublication();
        var event = createEvent(publication);

        handler.handleRequest(event, outputStream, context);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertFalse(logEntries.isEmpty());
    }

    @Test
    void shouldNotCreateLogEntryWhenConsumedEventHasResourceWithNewImageWhereResourceEventIsNull()
        throws BadRequestException, IOException {
        var publication = createPublication();
        Resource.fromPublication(publication).clearResourceEvent(resourceService);
        var event = createEvent(publication);

        handler.handleRequest(event, outputStream, context);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertTrue(logEntries.isEmpty());
    }

    @Test
    void shouldNotFailWhenWhenConsumedEventIsMissingNewImage()
        throws IOException {
        var event = createEvent(null);

        assertDoesNotThrow(() -> handler.handleRequest(event, outputStream, context));
    }

    private Publication createPublication() throws BadRequestException {
        var publication = randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication),
                                                 publication);
    }

    private static StreamRecord createPayload(Map<String, AttributeValue> oldImage,
                                              Map<String, AttributeValue> newImage) {
        var streamRecord = new StreamRecord();
        streamRecord.setOldImage(oldImage);
        streamRecord.setNewImage(newImage);
        return streamRecord;
    }

    private static DynamodbStreamRecord createDynamoRecord(StreamRecord payload) {
        var streamRecord = new DynamodbStreamRecord();
        streamRecord.setEventName(randomElement(OperationType.values()));
        streamRecord.setEventID(randomString());
        streamRecord.setAwsRegion(AWS_REGION);

        streamRecord.setDynamodb(payload);
        streamRecord.setEventSource(randomString());
        streamRecord.setEventVersion(randomString());
        return streamRecord;
    }

    private static DynamoEntry toDynamoEntry(Publication publication) {
        return nonNull(publication) ? new ResourceDao(Resource.fromPublication(publication)) : null;
    }

    private static StreamRecord createPayload(Publication oldImage, Publication newImage)
        throws JsonProcessingException {
        return createPayload(convertToAttributeValueMap(toDynamoEntry(oldImage)),
                             convertToAttributeValueMap(toDynamoEntry(newImage)));
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

    private InputStream createEvent(Publication publication) throws IOException {
        var dynamodbStreamRecord = createDynamoRecord(createPayload(null, publication));
        var blobUri = saveBlobInS3(dynamodbStreamRecord);
        var eventBody = new EventReference(DataEntryUpdateEvent.RESOURCE_UPDATE_EVENT_TOPIC, blobUri);
        return EventBridgeEventBuilder.sampleEvent(eventBody);
    }

    private URI saveBlobInS3(DynamodbStreamRecord blob) throws IOException {
        var json = attempt(() -> dtoObjectMapper.writeValueAsString(blob)).orElseThrow();
        return new S3Driver(s3Client, EVENTS_BUCKET)
                   .insertFile(UnixPath.of(UUID.randomUUID().toString()), json);
    }
}