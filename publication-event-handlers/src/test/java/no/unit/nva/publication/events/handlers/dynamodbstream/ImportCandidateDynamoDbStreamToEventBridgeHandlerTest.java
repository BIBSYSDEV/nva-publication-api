package no.unit.nva.publication.events.handlers.dynamodbstream;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.dynamodbstream.DynamoDbEventTestFactory.dynamodbEventEventWithSingleDynamoDbRecord;
import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toEntity;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UnixPath;
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

class ImportCandidateDynamoDbStreamToEventBridgeHandlerTest {

    public static final String EXPECTED_EXCEPTION_MESSAGE = "expected exception message";
    private FakeS3Client s3Client;
    private FakeContext context;
    private ImportCandidateDynamoDbStreamToEventBridgeHandler handler;
    private FakeS3Client failingS3Client;
    private FakeEventBridgeClient eventBridgeClient;

    public static Stream<Arguments> dynamoDbEventProvider() {
        var publication = randomPublication();
        return Stream.of(
            Arguments.of(dynamodbEventEventWithSingleDynamoDbRecord(Resource.fromPublication(publication), null)),
            Arguments.of(dynamodbEventEventWithSingleDynamoDbRecord(null, Resource.fromPublication(publication))),
            Arguments.of(dynamodbEventEventWithSingleDynamoDbRecord (Resource.fromPublication(publication),
                                                                    Resource.fromPublication(publication))));
    }

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
        eventBridgeClient = new FakeEventBridgeClient();
        this.handler = new ImportCandidateDynamoDbStreamToEventBridgeHandler(s3Client, eventBridgeClient);
    }

    @AfterEach
    void closeS3Client() {
        failingS3Client.close();
    }

    @ParameterizedTest
    @MethodSource("dynamoDbEventProvider")
    void shouldConvertDynamoRecordToDataEntryUpdateEventAndWriteToS3(DynamodbEvent event) {
        handler.handleRequest(event, context);
        var expectedDataEntryUpdateEvent = convertToDataEntryUpdateEvent(event.getRecords().getFirst());
        var actualDataEntryUpdateEvent = extractPersistedDataEntryUpdateEvent();

        assertThat(actualDataEntryUpdateEvent, is(equalTo(expectedDataEntryUpdateEvent)));
    }

    @Test
    void shouldThrowExceptionWhenWritingEventToS3Fails() {
        var event = dynamodbEventEventWithSingleDynamoDbRecord(Resource.fromPublication(randomPublication()), null);
        var failingS3Client = mock(S3Client.class);
        when(failingS3Client.putObject((PutObjectRequest) any(), (RequestBody) any())).thenThrow(new RuntimeException());
        var failingHandler = new ImportCandidateDynamoDbStreamToEventBridgeHandler(failingS3Client, eventBridgeClient);

        assertThrows(RuntimeException.class, () -> failingHandler.handleRequest(event, context));
    }

    @Test
    void shouldThrowExceptionWhenFailingOnSerializationOfEventBridgeDetails() {
        var event =
            dynamodbEventEventWithSingleDynamoDbRecord(DoiRequest.create(Resource.fromPublication(randomPublication()), UserInstance.create(randomString(), randomUri())), null);

        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, context));
    }

    private ImportCandidateDataEntryUpdate extractPersistedDataEntryUpdateEvent() {
        var s3Driver = new S3Driver(s3Client, EVENTS_BUCKET);
        return s3Driver.getFiles(UnixPath.ROOT_PATH)
                   .stream()
                   .map(attempt(json -> dtoObjectMapper.readValue(json, ImportCandidateDataEntryUpdate.class)))
                   .map(Try::orElseThrow)
                   .toList()
                   .getFirst();
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

    private ImportCandidateDataEntryUpdate convertToDataEntryUpdateEvent(DynamodbStreamRecord dynamoDbRecord) {
        return new ImportCandidateDataEntryUpdate(dynamoDbRecord.getEventName(),
                                                  getEntity(dynamoDbRecord.getDynamodb().getOldImage()),
                                                  getEntity(dynamoDbRecord.getDynamodb().getNewImage()));
    }

    private ImportCandidate getEntity(Map<String, AttributeValue> image) {
        return attempt(() -> toEntity(image)).toOptional()
                   .flatMap(Function.identity())
                   .map(Resource.class::cast)
                   .map(Resource::toImportCandidate)
                   .orElse(null);
    }
}