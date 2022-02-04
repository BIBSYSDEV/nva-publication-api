package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.expandresources.ExpandedDataEntriesPersistenceHandler.EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocumentConsumptionAttributes.DOI_REQUESTS_INDEX;
import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocumentConsumptionAttributes.MESSAGES_INDEX;
import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocumentConsumptionAttributes.RESOURCES_INDEX;
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
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedResourceConversation;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDataEntriesPersistenceHandlerTest {

    private static final ResourceExpansionService resourceExpansionService = fakeExpansionService();
    private static final String HELP_MESSAGE = String.format("%s should be compared for equality only as json "
                                                             + "objects", ExpandedResource.class.getSimpleName());
    private ExpandedDataEntriesPersistenceHandler handler;
    private S3Driver s3Reader;
    private S3Driver s3Writer;
    private URI eventUriInEventsBucket;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        var eventsBucket = new FakeS3Client();
        var indexBucket = new FakeS3Client();
        s3Reader = new S3Driver(eventsBucket, "eventsBucket");
        s3Writer = new S3Driver(indexBucket, "indexBucket");
        handler = new ExpandedDataEntriesPersistenceHandler(s3Reader, s3Writer);

        output = new ByteArrayOutputStream();
    }

    @ParameterizedTest(name = "should emit event containing S3 URI to persisted expanded resource")
    @MethodSource("expandedEntriesProvider")
    void shouldEmitEventContainingS3UriToPersistedExpandedResource(ExpandedDataEntry update)
        throws IOException {
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), update.toJsonString());
        EventReference outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getUri());
        assertThat(indexingEventPayload, is(not(emptyString())));
    }

    @ParameterizedTest(name = "should store entry containing the data referenced in the received event")
    @MethodSource("expandedEntriesProvider")
    void shouldStoreEntryContainingTheDataReferencedInTheReceivedEvent(ExpandedDataEntry update)
        throws IOException {
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), update.toJsonString());
        EventReference outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getUri());
        PersistedDocument indexDocument = PersistedDocument.fromJsonString(indexingEventPayload);
        assertThat(HELP_MESSAGE, indexDocument.getBody(), is(equalTo(update)));
    }

    @ParameterizedTest(name = "should store entry containing the general type (index name) of the persisted event")
    @MethodSource("entriesWithExpectedTypesProvider")
    void shouldStoreEntryContainingTheIndexNameForThePersistedEntry(
        PersistedEntryWithExpectedType expectedPersistedEntry)
        throws IOException {
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()),
                                                      expectedPersistedEntry.entry.toJsonString());
        EventReference outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getUri());
        PersistedDocument indexDocument = PersistedDocument.fromJsonString(indexingEventPayload);
        assertThat(indexDocument.getConsumptionAttributes().getIndex(), is(equalTo(expectedPersistedEntry.index)));
    }

    private static Stream<ExpandedDataEntry> expandedEntriesProvider() throws JsonProcessingException,
                                                                              NotFoundException {
        return Stream.of(randomResource(), randomDoiRequest(), randomResourceConversation());
    }

    private static Stream<PersistedEntryWithExpectedType> entriesWithExpectedTypesProvider()
        throws JsonProcessingException, NotFoundException {
        return Stream.of(
            new PersistedEntryWithExpectedType(randomResource(), RESOURCES_INDEX),
            new PersistedEntryWithExpectedType(randomDoiRequest(), DOI_REQUESTS_INDEX),
            new PersistedEntryWithExpectedType(randomResourceConversation(), MESSAGES_INDEX));
    }

    private static ExpandedResource randomResource() throws JsonProcessingException {
        var publication = PublicationGenerator.randomPublication();
        return ExpandedResource.fromPublication(publication);
    }

    private static ExpandedDoiRequest randomDoiRequest() throws NotFoundException {
        DoiRequest doiRequest = DoiRequest.newDoiRequestForResource(
            Resource.fromPublication(PublicationGenerator.randomPublication()));
        return ExpandedDoiRequest.create(doiRequest, resourceExpansionService);
    }

    private static ExpandedResourceConversation randomResourceConversation() throws NotFoundException {
        var publication = PublicationGenerator.randomPublication();
        //TODO: create proper ExpandedResourceConversation
        var expandedResourceConversation = new ExpandedResourceConversation();
        expandedResourceConversation.setPublicationSummary(PublicationSummary.create(publication));
        expandedResourceConversation.setPublicationIdentifier(publication.getIdentifier());
        return expandedResourceConversation;
    }

    private static ResourceExpansionServiceImpl fakeExpansionService() {
        return new ResourceExpansionServiceImpl(null, null, null) {
            @Override
            public Set<URI> getOrganizationIds(DataEntry dataEntry) {
                return Set.of(randomUri());
            }
        };
    }

    private EventReference sendEvent() throws JsonProcessingException {
        EventReference eventReference =
            new EventReference(EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC, eventUriInEventsBucket);
        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventReference);
        handler.handleRequest(event, output, mock(Context.class));
        return objectMapper.readValue(output.toString(), EventReference.class);
    }

    private static class PersistedEntryWithExpectedType {

        final ExpandedDataEntry entry;
        final String index;

        public PersistedEntryWithExpectedType(ExpandedDataEntry databaseEntry, String index) {
            this.entry = databaseEntry;
            this.index = index;
        }
    }
}