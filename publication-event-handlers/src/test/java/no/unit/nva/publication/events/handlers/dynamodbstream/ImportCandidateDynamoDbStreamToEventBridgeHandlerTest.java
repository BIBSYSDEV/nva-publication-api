package no.unit.nva.publication.events.handlers.dynamodbstream;

import static no.unit.nva.commons.json.JsonUtils.dtoObjectMapper;
import static no.unit.nva.model.testing.EntityDescriptionBuilder.randomEntityDescription;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.EVENTS_BUCKET;
import static no.unit.nva.publication.events.handlers.dynamodbstream.DynamoDbEventTestFactory.dynamodbEventEventWithSingleDynamoDbRecord;
import static no.unit.nva.publication.events.handlers.fanout.DynamodbStreamRecordDaoMapper.toImportCandidate;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportStatusFactory;
import no.unit.nva.model.Organization;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.instancetypes.journal.JournalArticle;
import no.unit.nva.publication.events.bodies.ImportCandidateDataEntryUpdate;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.RandomDataGenerator;
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
        var importCandidate = randomImportCandidate();
        return Stream.of(
            Arguments.of(dynamodbEventEventWithSingleDynamoDbRecord(importCandidate, null)),
            Arguments.of(dynamodbEventEventWithSingleDynamoDbRecord(null, importCandidate)),
            Arguments.of(dynamodbEventEventWithSingleDynamoDbRecord(importCandidate,
                                                                    importCandidate)));
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
        var event = dynamodbEventEventWithSingleDynamoDbRecord(randomImportCandidate(), null);
        var failingS3Client = mock(S3Client.class);
        when(failingS3Client.putObject((PutObjectRequest) any(), (RequestBody) any())).thenThrow(new RuntimeException());
        var failingHandler = new ImportCandidateDynamoDbStreamToEventBridgeHandler(failingS3Client, eventBridgeClient);

        assertThrows(RuntimeException.class, () -> failingHandler.handleRequest(event, context));
    }

    @Test
    void shouldNotEmitEventWhenConsumedBlobIsEmpty() {
        var event = dynamodbEventEventWithSingleDynamoDbRecord(null, null);

        var eventReference = handler.handleRequest(event, context);

        assertNull(eventReference);
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

    private static ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription(JournalArticle.class))
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(RandomDataGenerator.randomUri()).build())
                   .withIdentifier(SortableIdentifier.next())
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), RandomDataGenerator.randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    private ImportCandidate getEntity(Map<String, AttributeValue> image) {
        return attempt(() -> toImportCandidate(image)).toOptional().flatMap(Function.identity()).orElse(null);
    }
}