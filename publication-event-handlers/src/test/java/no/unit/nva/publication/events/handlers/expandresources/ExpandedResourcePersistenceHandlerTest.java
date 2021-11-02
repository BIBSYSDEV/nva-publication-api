package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.dynamoImageSerializerRemovingEmptyFields;
import static no.unit.nva.publication.storage.model.Message.supportMessage;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDatabaseEntry;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationGenerator;
import no.unit.nva.publication.events.EventPayload;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedResourcePersistenceHandlerTest {

    private static final ResourceExpansionService resourceExpansionService = fakeExpansionService();
    private ExpandedResourcePersistenceHandler handler;
    private S3Driver s3Reader;
    private S3Driver s3Writer;
    private URI eventUriInEventsBucket;
    private ByteArrayOutputStream output;

    public static Stream<ExpandedDatabaseEntry> expandedEntriesProvider() throws JsonProcessingException {
        return Stream.of(randomResource(), randomDoiRequest(), randomMessage());
    }

    public static Stream<PersistedEntryWithExpectedType> entriesWithExpectedTypesProvider()
        throws JsonProcessingException {
        return Stream.of(
            new PersistedEntryWithExpectedType(randomResource(), IndexDocument.RESOURCE_UPDATE),
            new PersistedEntryWithExpectedType(randomDoiRequest(), IndexDocument.DOI_REQUEST_UPDATE),
            new PersistedEntryWithExpectedType(randomMessage(), IndexDocument.MESSAGE_UPDATE));
    }

    public static ExpandedResource randomResource() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        return ExpandedResource.fromPublication(publication);
    }

    public static ExpandedDoiRequest randomDoiRequest() {
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(
            Resource.fromPublication(PublicationGenerator.randomPublication()));
        return ExpandedDoiRequest.create(doiRequest, resourceExpansionService);
    }

    @BeforeEach
    public void init() {
        var eventsBucket = new FakeS3Client();
        var indexBucket = new FakeS3Client();
        s3Reader = new S3Driver(eventsBucket, "eventsBucket");
        s3Writer = new S3Driver(indexBucket, "indexBucket");
        handler = new ExpandedResourcePersistenceHandler(s3Reader, s3Writer);

        output = new ByteArrayOutputStream();
    }

    @ParameterizedTest(name = "should emit event containing S3 URI to persisted expanded resource")
    @MethodSource("expandedEntriesProvider")
    void shouldEmitEventContainingS3UriToPersistedExpandedResource(ExpandedDatabaseEntry update)
        throws IOException {
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), update.toJsonString());
        EventPayload outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getPayloadUri());
        assertThat(indexingEventPayload, is(not(emptyString())));
    }

    @ParameterizedTest(name = "should store entry containing the data referenced in the received event")
    @MethodSource("expandedEntriesProvider")
    void shouldStoreEntryContainingTheDataReferencedInTheReceivedEvent(ExpandedDatabaseEntry update)
        throws IOException {
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), update.toJsonString());
        EventPayload outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getPayloadUri());
        IndexDocument indexDocument = IndexDocument.fromJsonString(indexingEventPayload);
        assertThat(indexDocument.getBody(), is(equalTo(update)));
    }

    @ParameterizedTest(name = "should store entry containing the general type (index namde) of the persisted event")
    @MethodSource("entriesWithExpectedTypesProvider")
    void shouldStoreEntryContainingTheGeneralTypeOfThePersistedEntry(
        PersistedEntryWithExpectedType expectedPersistedEntry)
        throws IOException {
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()),
                                                      expectedPersistedEntry.entry.toJsonString());
        EventPayload outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getPayloadUri());
        IndexDocument indexDocument = IndexDocument.fromJsonString(indexingEventPayload);
        assertThat(indexDocument.getType(), is(equalTo(expectedPersistedEntry.type)));
    }

    private static ExpandedMessage randomMessage() {
        var randomUser = new UserInstance(randomString(), randomUri());
        var publication = PublicationGenerator.randomPublication();
        var clock = Clock.systemDefaultZone();
        var message = supportMessage(randomUser, publication, randomString(), SortableIdentifier.next(), clock);
        return ExpandedMessage.create(message, resourceExpansionService);
    }

    private static ResourceExpansionServiceImpl fakeExpansionService() {
        return new ResourceExpansionServiceImpl(null, null) {
            @Override
            public Set<URI> getOrganizationIds(String username) {
                return Set.of(randomUri());
            }
        };
    }

    private EventPayload sendEvent() throws JsonProcessingException {
        EventPayload eventPayload = EventPayload.resourcesUpdateEvent(eventUriInEventsBucket);
        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventPayload);
        handler.handleRequest(event, output, mock(Context.class));
        return dynamoImageSerializerRemovingEmptyFields.readValue(output.toString(), EventPayload.class);
    }

    private static class PersistedEntryWithExpectedType {

        final ExpandedDatabaseEntry entry;
        final String type;

        public PersistedEntryWithExpectedType(ExpandedDatabaseEntry databaseEntry, String type) {
            this.entry = databaseEntry;
            this.type = type;
        }
    }
}