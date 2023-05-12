package no.unit.nva.publication.events.handlers.dynamodbstream;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.AWS_REGION;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.Record;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.StreamRecord;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class DynamodbStreamToEventBridgeHandlerTest {

    public static final String DYNAMODB_UPDATE_EVENT_TOPIC = "PublicationService.Database.Update";
    
    public static final String EXPECTED_EXCEPTION_MESSAGE = "expected exception message";
    private FakeS3Client s3Client;
    private FakeContext context;
    private DynamodbStreamToEventBridgeHandler handler;
    private FakeEventBridgeClient eventBridgeClient;
    
    @BeforeEach
    public void init() {
        this.s3Client = new FakeS3Client();
        createFailingS3Client();
        this.eventBridgeClient = new FakeEventBridgeClient();
        this.context = new FakeContext() {
            @Override
            public String getInvokedFunctionArn() {
                return randomString();
            }
        };
        this.handler = new DynamodbStreamToEventBridgeHandler(s3Client, eventBridgeClient, DYNAMODB_UPDATE_EVENT_TOPIC);
    }
    
    @Test
    void shouldWriteEachDynamoRecordOfDynamoDbEventInS3() {
        var event = randomEventWithMultipleDynamoRecords();
        handler.handleRequest(event, context);
        var expectedEventId = extractIdentifiersForExpectedStoredEntry(event);
        var actualEventId = extractIdentifierFromActualStoredEntry();
        assertThat(actualEventId, is(equalTo(expectedEventId)));
    }
    
    @Test
    void shouldThrowExceptionWhenStoringAnyEventRecordInS3Fails() {
        var event = randomEventWithMultipleDynamoRecords();
        handler = new DynamodbStreamToEventBridgeHandler(createFailingS3Client(), eventBridgeClient, DYNAMODB_UPDATE_EVENT_TOPIC);
        Executable action = () -> handler.handleRequest(event, context);
        var exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(EXPECTED_EXCEPTION_MESSAGE));
    }
    
    @Test
    void shouldEmitOneEventPerDynamoDbStreamRecordContainedInDynamoDbEvent() {
        var event = randomEventWithMultipleDynamoRecords();
        handler.handleRequest(event, context);
        var emittedFilePaths = eventBridgeClient.getRequestEntries()
                                   .stream()
                                   .map(PutEventsRequestEntry::detail)
                                   .map(
                                       attempt(json -> JsonUtils.dtoObjectMapper.readValue(json, EventReference.class)))
                                   .map(Try::orElseThrow)
                                   .map(EventReference::getUri)
                                   .map(UriWrapper::fromUri)
                                   .map(UriWrapper::toS3bucketPath)
                                   .collect(Collectors.toSet());
        var expectedFilePaths = new HashSet<>(new S3Driver(s3Client, EVENTS_BUCKET).listAllFiles(UnixPath.ROOT_PATH));
        assertThat(emittedFilePaths, is(equalTo(expectedFilePaths)));
    }
    
    private DynamodbEvent randomEventWithMultipleDynamoRecords() {
        var event = new DynamodbEvent();
        var records = List.of(randomDynamoRecord(), randomDynamoRecord(), randomDynamoRecord());
        event.setRecords(records);
        return event;
    }
    
    private Set<String> extractIdentifierFromActualStoredEntry() {
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        return s3Driver.getFiles(UnixPath.ROOT_PATH)
                   .stream()
                   .map(attempt(json -> JsonUtils.dtoObjectMapper.readValue(json, DynamodbStreamRecord.class)))
                   .map(Try::orElseThrow)
                   .map(Record::getEventID)
                   .collect(Collectors.toSet());
    }
    
    private Set<String> extractIdentifiersForExpectedStoredEntry(DynamodbEvent event) {
        return event.getRecords()
                   .stream()
                   .map(Record::getEventID)
                   .collect(Collectors.toSet());
    }
    
    private DynamodbEvent.DynamodbStreamRecord randomDynamoRecord() {
        var record = new DynamodbStreamRecord();
        record.setEventName(randomElement(OperationType.values()));
        record.setEventID(randomString());
        record.setAwsRegion(AWS_REGION);
        record.setDynamodb(randomPayload());
        record.setEventSource(randomString());
        record.setEventVersion(randomString());
        return record;
    }
    
    private StreamRecord randomPayload() {
        var record = new StreamRecord();
        record.setOldImage(randomDynamoPayload());
        record.setNewImage(randomDynamoPayload());
        return record;
    }
    
    private Map<String, AttributeValue> randomDynamoPayload() {
        var value = new AttributeValue(randomString());
        return Map.of(randomString(), value);
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
}
