package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.ApplicationConstants.EVENT_BUS_NAME;
import static no.unit.nva.publication.s3imports.S3ImportsConfig.s3ImportsMapper;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.events.DeleteEntryEvent;
import no.unit.nva.stubs.FakeEventBridgeClient;
import no.unit.nva.stubs.FakeS3Client;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.logutils.LogUtils;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.Every;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class DeleteEntriesEventEmitterTest {

    public static final Context context = mock(Context.class);
    public static final String HARDCODED_PATH = "reports/date/";
    private final String bucketName = "some_bucket";

    private FakeEventBridgeClient eventBridgeClient;
    private FakeS3Client s3Client;
    private DeleteEntriesEventEmitter handler;

    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void init() {
        s3Client = new FakeS3Client();
        eventBridgeClient = new FakeEventBridgeClient(EVENT_BUS_NAME);
        handler = new DeleteEntriesEventEmitter(s3Client, eventBridgeClient);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnListOfImportedPublicationsFromS3() {
        var expectedIdentifiers = createRandomIdentifiers();
        putObjectsInBucket(expectedIdentifiers);
        var input = toInputStream(createInputEventForFile(URI.create("s3://brage-migration-reports-750639270376")));
        handler.handleRequest(input, outputStream, context);
        List<DeleteEntryEvent> eventBodiesOfEmittedEventReferences = collectBodiesOfEmittedEventReferences();
        assertThat(eventBodiesOfEmittedEventReferences,
                   (Every.everyItem(HasPropertyWithValue.hasProperty("topic",
                                                                     is(equalTo(
                                                                         DeleteEntryEvent.EVENT_TOPIC))))));
        var actualEmittedIdentifier =
            eventBodiesOfEmittedEventReferences.stream()
                .map(DeleteEntriesEventEmitterTest::getIdentifier)
                .collect(
                    Collectors.toList());
        assertThat(actualEmittedIdentifier, containsInAnyOrder(expectedIdentifiers.toArray()));
    }

    @Test
    void shouldLogErrorWhenEmittingEventsFails() {
        var appender = LogUtils.getTestingAppender(DeleteEntriesEventEmitter.class);
        eventBridgeClient = new FakeEventBridgeClientThatFailsAllPutEvents(EVENT_BUS_NAME);
        handler = new DeleteEntriesEventEmitter(s3Client, eventBridgeClient);
        var identifiers = createRandomIdentifiers();
        putObjectsInBucket(identifiers);
        var input = toInputStream(createInputEventForFile(URI.create("s3://brage-migration-reports-750639270376")));
        handler.handleRequest(input, outputStream, context);
        assertThat(appender.getMessages(), containsString(identifiers.get(0)));
    }

    private static String getIdentifier(DeleteEntryEvent deleteEntryEvent) {
        return deleteEntryEvent.getIdentifier().toString();
    }

    private List<DeleteEntryEvent> collectBodiesOfEmittedEventReferences() {
        return eventBridgeClient.getRequestEntries()
                   .stream()
                   .map(PutEventsRequestEntry::detail)
                   .map(DeleteEntryEvent::fromJson)
                   .collect(Collectors.toList());
    }

    private AwsEventBridgeEvent<EventReference> createInputEventForFile(URI fileUri) {
        var eventReference = new EventReference(null,
                                                null,
                                                fileUri,
                                                Instant.now());
        var request = new AwsEventBridgeEvent<EventReference>();

        request.setDetail(eventReference);
        return request;
    }

    private InputStream toInputStream(AwsEventBridgeEvent<EventReference> request) {
        return attempt(() -> s3ImportsMapper.writeValueAsString(request))
                   .map(IoUtils::stringToStream)
                   .orElseThrow();
    }

    private List<String> createRandomIdentifiers() {
        return IntStream.range(0, 2000)
                   .boxed()
                   .map(index -> SortableIdentifier.next().toString())
                   .collect(Collectors.toList());
    }

    private void putObjectsInBucket(List<String> keys) {
        keys.forEach(object -> s3Client.putObject(
            PutObjectRequest.builder().bucket(bucketName).key(HARDCODED_PATH + object).build(),
            RequestBody.empty()));
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
