package no.unit.nva.publication.s3imports;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.RequestParametersEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.ResponseElementsEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3BucketEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3Entity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.S3ObjectEntity;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification.UserIdentityEntity;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.RandomDataGenerator;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.hamcrest.Matcher;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.Every;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

public class DeleteImportCandidatesEventEmitterTest {

    public static final Context CONTEXT = null;
    public static final long SOME_FILE_SIZE = 100L;
    public static final InputStream EVENT = IoUtils.inputStreamFromResources(
        "delete_scopus_identifier_list.txt");
    private static final String INPUT_BUCKET_NAME = "some-input-bucket-name";
    private static final RequestParametersEntity EMPTY_REQUEST_PARAMETERS = null;
    private static final ResponseElementsEntity EMPTY_RESPONSE_ELEMENTS = null;
    private static final UserIdentityEntity EMPTY_USER_IDENTITY = null;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private FakeEventBridgeClient eventBridgeClient;
    private DeleteImportCandidatesEventEmitter handler;

    @BeforeEach
    public void init() {
        s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        eventBridgeClient = new FakeEventBridgeClient(ApplicationConstants.EVENT_BUS_NAME);
        handler = new DeleteImportCandidatesEventEmitter(s3Client, eventBridgeClient);
    }

    @Test
    void shouldEmitEventsForEveryScopusIdentifierInDeletionList() throws IOException {
        var s3Event = createS3Event(RandomDataGenerator.randomString());
        handler.handleRequest(s3Event, CONTEXT);
        var eventBodiesOfEmittedEventReferences = collectBodiesOfEmittedEventReferences();
        assertThat(eventBodiesOfEmittedEventReferences, everyItemIs());
    }

    @Test
    void shouldLogNotEmittedEvents() throws IOException {
        var s3Event = createS3Event(RandomDataGenerator.randomString());
        eventBridgeClient = new DeleteImportCandidatesEventEmitterTest.FakeEventBridgeClientThatFailsAllPutEvents(
            ApplicationConstants.EVENT_BUS_NAME);
        s3Driver = new S3Driver(s3Client, "ignoredValue");
        handler = new DeleteImportCandidatesEventEmitter(s3Client, eventBridgeClient);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(s3Event, CONTEXT);
        assertThat(appender.getMessages(), containsString("Not emitted events"));
    }

    private static Matcher<Iterable<?>> everyItemIs() {
        return Every.everyItem(HasPropertyWithValue.hasProperty(ImportCandidateDeleteEvent.TOPIC,
                                                                is(equalTo(
                                                                    ImportCandidateDeleteEvent.EVENT_TOPIC))));
    }

    private S3Event createS3Event(String expectedObjectKey) throws IOException {
        var eventNotification = new S3EventNotificationRecord(RandomDataGenerator.randomString(),
                                                              RandomDataGenerator.randomString(),
                                                              RandomDataGenerator.randomString(),
                                                              randomDate(),
                                                              RandomDataGenerator.randomString(),
                                                              EMPTY_REQUEST_PARAMETERS,
                                                              EMPTY_RESPONSE_ELEMENTS,
                                                              createS3Entity(expectedObjectKey),
                                                              EMPTY_USER_IDENTITY);
        s3Driver.insertFile(UnixPath.of(expectedObjectKey), EVENT);
        return new S3Event(List.of(eventNotification));
    }

    private S3Entity createS3Entity(String expectedObjectKey) {
        var bucket = new S3BucketEntity(INPUT_BUCKET_NAME, EMPTY_USER_IDENTITY, RandomDataGenerator.randomString());
        var object = new S3ObjectEntity(expectedObjectKey,
                                        SOME_FILE_SIZE,
                                        RandomDataGenerator.randomString(),
                                        RandomDataGenerator.randomString(),
                                        RandomDataGenerator.randomString());
        var schemaVersion = RandomDataGenerator.randomString();
        return new S3Entity(RandomDataGenerator.randomString(), bucket, object, schemaVersion);
    }

    private List<ImportCandidateDeleteEvent> collectBodiesOfEmittedEventReferences() {
        return eventBridgeClient.getRequestEntries()
                   .stream()
                   .map(PutEventsRequestEntry::detail)
                   .map(event -> attempt(() ->JsonUtils.dtoObjectMapper.readValue(event,
                                                                                ImportCandidateDeleteEvent.class)).orElseThrow())
                   .collect(Collectors.toList());
    }

    private String randomDate() {
        return Instant.now().toString();
    }

    static class FakeEventBridgeClientThatFailsAllPutEvents extends FakeEventBridgeClient {

        public FakeEventBridgeClientThatFailsAllPutEvents(String eventBusName) {
            super(eventBusName);
        }

        @Override
        public PutEventsResponse putEvents(PutEventsRequest putEventsRequest) throws
                                                                              AwsServiceException,
                                                                              SdkClientException {
            return PutEventsResponse.builder().failedEntryCount(putEventsRequest.entries().size()).build();
        }
    }
}
