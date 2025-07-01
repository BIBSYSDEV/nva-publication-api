package no.unit.nva.publication.events.handlers.dynamodbstream;

import static java.util.Objects.nonNull;
import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.AWS_REGION;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toEntity;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import com.fasterxml.jackson.databind.JavaType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.FakeSqsClient;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.Environment;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UnixPath;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class DynamodbStreamToEventBridgeHandlerTest {

    public static final String EXPECTED_EXCEPTION_MESSAGE = "expected exception message";
    private FakeS3Client s3Client;
    private FakeContext context;
    private DynamodbStreamToEventBridgeHandler handler;
    private FakeSqsClient fakeSqsClient;
    private FakeS3Client failingS3Client;
    private FakeEventBridgeClient eventBridgeClient;

    @BeforeEach
    public void init() {
        this.s3Client = new FakeS3Client();
        failingS3Client = createFailingS3Client();
        this.context = new FakeContext() {
            @Override
            public String getInvokedFunctionArn() {
                return randomString();
            }
        };
        fakeSqsClient = new FakeSqsClient();
        this.eventBridgeClient = new FakeEventBridgeClient();
        this.handler = new DynamodbStreamToEventBridgeHandler(s3Client, eventBridgeClient, fakeSqsClient, new Environment());
    }

    @AfterEach
    void closeS3Client() {
        failingS3Client.close();
    }

    public static Stream<Arguments> dynamoDbEventProvider() {
        var publication = PublicationGenerator.randomPublication();
        return
            Stream.of(Arguments.of(randomEventWithSingleDynamoRecord(OperationType.REMOVE,
                                                                     Resource.fromPublication(publication),
                                                                     null),
                                   "PublicationService.Resource.Deleted"),
                      Arguments.of(randomEventWithSingleDynamoRecord(OperationType.INSERT,
                                                                     null,
                                                                     Resource.fromPublication(publication)),
                                   "PublicationService.Resource.Update"),
                      Arguments.of(randomEventWithSingleDynamoRecord(OperationType.MODIFY,
                                                                     Resource.fromPublication(publication),
                                                                     Resource.fromPublication(randomPublication())),
                                   "PublicationService.Resource.Update"));
    }

    @ParameterizedTest
    @MethodSource("dynamoDbEventProvider")
    void shouldConvertDynamoRecordToDataEntryUpdateEventAndWriteToS3(DynamodbEvent event, String topic) {
        handler.handleRequest(event, context);
        var expectedDataEntryUpdateEvent = convertToDataEntryUpdateEvent(event.getRecords().getFirst());
        var actualDataEntryUpdateEvent = extractPersistedDataEntryUpdateEvent();

        assertThat(actualDataEntryUpdateEvent.getTopic(), is(equalTo(topic)));
        assertThat(actualDataEntryUpdateEvent, is(equalTo(expectedDataEntryUpdateEvent)));
    }

    @Test
    void shouldPlaceFailedEntryOnRecoveryQueueWhenStoringAnyEventRecordInS3Fails() {
        var event = randomEventWithSingleDynamoRecord(OperationType.MODIFY,
                                                      Resource.fromPublication(randomPublication()),
                                                      Resource.fromPublication(randomPublication()));
        handler = new DynamodbStreamToEventBridgeHandler(createFailingS3Client(), eventBridgeClient, fakeSqsClient,
                                                         new Environment());
        handler.handleRequest(event, context);
        var recoveryMessage = fakeSqsClient.getDeliveredMessages().getFirst();
        assertThat(recoveryMessage.messageAttributes().get("id"), Matchers.is(notNullValue()));
    }

    @Test
    void shouldEmitEventWhenDataEntryUpdateEventForFileEntryDoesNotHaveNewImage() {
        var event = randomEventWithSingleDynamoRecord(OperationType.REMOVE, randomFileEntry(), null);
        handler.handleRequest(event, context);
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        var persistedEvents = s3Driver.getFiles(UnixPath.ROOT_PATH);

        assertFalse(persistedEvents.isEmpty());
    }
    
    @Test
    void shouldProcessMultipleRecordsInEvent() {
        var numberOfEvents = 10;
        var event = randomEventWithMultipleRecords(numberOfEvents);
        handler.handleRequest(event, context);
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        var persistedEvents = s3Driver.getFiles(UnixPath.ROOT_PATH);

        assertEquals(numberOfEvents, persistedEvents.size());
    }

    @Test
    void shouldProcessRestOfTheRecordsWhenSingleRecordFailsInEvent() {
        var numberOfEvents = 10;
        var event = randomEventWithMultipleRecords(numberOfEvents);
        var singleFailureS3Client = new FailingS3Client();
        handler = new DynamodbStreamToEventBridgeHandler(singleFailureS3Client, eventBridgeClient, fakeSqsClient,
                                                         new Environment());
        handler.handleRequest(event, context);

        assertEquals(numberOfEvents - 1, singleFailureS3Client.getSuccessRequest().size());
    }

    private static FileEntry randomFileEntry() {
        return FileEntry.create(randomOpenFile(),
                                SortableIdentifier.next(), UserInstance.create(randomString(), randomUri()));
    }

    private static DynamodbEvent randomEventWithSingleDynamoRecord(OperationType operationType,
                                                                   Entity oldImage,
                                                                   Entity newImage) {
        var event = new DynamodbEvent();
        var record = randomRecord(randomDynamoRecord(operationType), toDynamoDbFormat(oldImage),
                                  toDynamoDbFormat(newImage));
        event.setRecords(List.of(record));
        return event;
    }

    private static DynamodbStreamRecord randomRecord(DynamodbStreamRecord operationType,
                                                     Map<String, AttributeValue> oldImage,
                                                     Map<String, AttributeValue> newImage) {
        var record = operationType;
        record.getDynamodb().setOldImage(oldImage);
        record.getDynamodb().setNewImage(newImage);
        return record;
    }

    private static DynamodbEvent randomEventWithMultipleRecords(int numberOfEvents) {
        var event = new DynamodbEvent();
        var records = IntStream.range(0, numberOfEvents).boxed()
                          .map(i -> randomRecord(randomDynamoRecord(OperationType.MODIFY),
                                                 toDynamoDbFormat(Resource.fromPublication(randomPublication())),
                                                 toDynamoDbFormat(Resource.fromPublication(randomPublication()))))
                          .toList();
        event.setRecords(records);
        return event;
    }

    private static Map<String, AttributeValue> toDynamoDbFormat(Entity publication) {
        return nonNull(publication) ? publicationDynamoDbFormat(publication) : null;
    }

    private static Map<String, AttributeValue> publicationDynamoDbFormat(Entity publication) {
        var dao = publication.toDao().toDynamoFormat();
        var string = attempt(() -> dtoObjectMapper.writeValueAsString(dao)).orElseThrow();
        return (Map<String, AttributeValue>) attempt(() -> dtoObjectMapper.readValue(string,
                                                                                     dynamoMapStructureAsJacksonType())).orElseThrow();
    }

    private static JavaType dynamoMapStructureAsJacksonType() {
        return dtoObjectMapper.getTypeFactory().constructParametricType(Map.class, String.class, AttributeValue.class);
    }

    private DataEntryUpdateEvent extractPersistedDataEntryUpdateEvent() {
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        return s3Driver.getFiles(UnixPath.ROOT_PATH)
                   .stream()
                   .map(attempt(json -> dtoObjectMapper.readValue(json, DataEntryUpdateEvent.class)))
                   .map(Try::orElseThrow)
                   .toList().getFirst();
    }

    private static DynamodbEvent.DynamodbStreamRecord randomDynamoRecord(OperationType operationType) {
        var streamRecord = new DynamodbStreamRecord();
        streamRecord.setEventName(operationType);
        streamRecord.setEventID(randomString());
        streamRecord.setAwsRegion(AWS_REGION);
        var dynamodb = new StreamRecord();
        streamRecord.setDynamodb(dynamodb);
        streamRecord.setEventSource(randomString());
        streamRecord.setEventVersion(randomString());
        return streamRecord;
    }

    private FakeS3Client createFailingS3Client() {
        return new FakeS3Client() {
            @SuppressWarnings("PMD.CloseResource")
            @Override
            public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) {
                throw new RuntimeException(EXPECTED_EXCEPTION_MESSAGE);
            }
        };
    }

    public static class FailingS3Client implements S3Client {
        private boolean hasFailed = false;

        @Override
        public String serviceName() {
            return "";
        }

        @Override
        public void close() {

        }

        public List<PutObjectRequest> getSuccessRequest() {
            return successRequest;
        }

        @SuppressWarnings("PMD.CloseResource")
        @Override
        public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody) {
            if (!hasFailed) {
                hasFailed = true;
                throw new RuntimeException(EXPECTED_EXCEPTION_MESSAGE);
            }
            successRequest.add(putObjectRequest);
            return null;
        }

        private final List<PutObjectRequest> successRequest = new ArrayList<>();
    }

    private DataEntryUpdateEvent convertToDataEntryUpdateEvent(DynamodbStreamRecord dynamoDbRecord) {
        return new DataEntryUpdateEvent(dynamoDbRecord.getEventName(),
                                        getEntity(dynamoDbRecord.getDynamodb().getOldImage()),
                                        getEntity(dynamoDbRecord.getDynamodb().getNewImage()));
    }

    private Entity getEntity(Map<String, AttributeValue> image) {
        return attempt(() -> toEntity(image))
                   .toOptional()
                   .flatMap(Function.identity())
                   .orElse(null);
    }
}
