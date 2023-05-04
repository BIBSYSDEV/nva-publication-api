package no.unit.nva.publication.events.handlers.expandresources;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.bodies.DataEntryUpdateEvent.RESOURCE_UPDATE_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler.EMPTY_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler.EXPANDED_ENTRY_DELETE_EVENT_TOPIC;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExpandDataEntriesHandlerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final String EXPECTED_ERROR_MESSAGE = "expected error message";
    public static final String IDENTIFIER_IN_RESOURCE_FILE = "017ca2670694-37f2c1a7-0105-452c-b7b3-1d90a44a11c0";
    public static final Clock CLOCK = Clock.systemDefaultZone();
    public static final Publication DELETED_RESOURCE = null;
    public static final Object EMPTY_IMAGE = null;
    private static final URI AFFILIATION_URI_FOUND_IN_FAKE_PERSON_API_RESPONSE =
        URI.create("https://api.cristin.no/v2/units/194.63.10.0");
    private ByteArrayOutputStream output;
    private ExpandDataEntriesHandler expandResourceHandler;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        this.output = new ByteArrayOutputStream();
        s3Client = new FakeS3Client();
        resourceService = new ResourceService(client, CLOCK);
        var ticketService = new TicketService(client);

        insertPublicationWithIdentifierAndAffiliationAsTheOneFoundInResources();

        var mockUriRetriever = mock(UriRetriever.class);
        when(mockUriRetriever.getRawContent(any(), any())).thenReturn(Optional.empty());

        ResourceExpansionService resourceExpansionService =
            new ResourceExpansionServiceImpl(resourceService, ticketService, mockUriRetriever);

        this.expandResourceHandler = new ExpandDataEntriesHandler(s3Client, resourceExpansionService);
        this.s3Driver = new S3Driver(s3Client, "ignoredForFakeS3Client");
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, mode = INCLUDE, names = {"PUBLISHED", "PUBLISHED_METADATA"})
    void shouldProduceAnExpandedDataEntryWhenInputHasNewImage(PublicationStatus status) throws IOException {
        var oldImage = createPublicationWithStatus(status);
        var newImage = createUpdatedVersionOfPublication(oldImage);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, newImage);
        expandResourceHandler.handleRequest(request, output, CONTEXT);
        var response = parseHandlerResponse();
        var eventBlobStoredInS3 = s3Driver.readEvent(response.getUri());
        var blobObject = JsonUtils.dtoObjectMapper.readValue(eventBlobStoredInS3, ExpandedDataEntry.class);
        assertThat(blobObject.identifyExpandedEntry(), is(equalTo(newImage.getIdentifier())));
    }

    @ParameterizedTest
    @EnumSource(value = PublicationStatus.class, mode = EXCLUDE, names = {"PUBLISHED", "PUBLISHED_METADATA", "DELETED"})
    void shouldNotProduceEntryWhenNotPublishedOrDeletedEntry(PublicationStatus status) throws IOException {
        var oldImage = createPublicationWithStatus(status);
        var newImage = createUpdatedVersionOfPublication(oldImage);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, newImage);
        expandResourceHandler.handleRequest(request, output, CONTEXT);
        var response = parseHandlerResponse();
        assertThat(response, is(equalTo(emptyEvent(response.getTimestamp()))));
    }

    @Test
    void shouldNotProduceAnExpandedDataEntryWhenInputHasNoNewImage() throws IOException {
        var oldImage = createPublicationWithStatus(PUBLISHED);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, DELETED_RESOURCE);
        expandResourceHandler.handleRequest(request, output, CONTEXT);
        var response = parseHandlerResponse();
        assertThat(response, is(equalTo(emptyEvent(response.getTimestamp()))));
    }

    @Test
    void shouldLogFailingExpansionNotThrowExceptionAndEmitEmptyEvent() throws IOException {
        var oldImage = createPublicationWithStatus(PUBLISHED);
        var newImage = createUpdatedVersionOfPublication(oldImage);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, newImage);

        var logger = LogUtils.getTestingAppenderForRootLogger();

        expandResourceHandler = new ExpandDataEntriesHandler(s3Client, createFailingService());
        expandResourceHandler.handleRequest(request, output, CONTEXT);

        assertThat(logger.getMessages(), containsString(EXPECTED_ERROR_MESSAGE));
        assertThat(logger.getMessages(), containsString(newImage.getIdentifier().toString()));
    }

    @Test
    void shouldIgnoreAndNotCreateEnrichmentEventForDraftResources() throws IOException {
        var oldImage = createPublicationWithStatus(DRAFT);
        var newImage = createUpdatedVersionOfPublication(oldImage);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, newImage);

        expandResourceHandler.handleRequest(request, output, CONTEXT);
        var eventReference = parseHandlerResponse();
        assertThat(eventReference, is(equalTo(emptyEvent(eventReference.getTimestamp()))));
    }

    @Test
    void shouldIgnoreAndNotCreateEnrichmentEventForDoiRequestsOfDraftResources() throws IOException {
        var newImage = doiRequestForDraftResource();
        var event = emulateEventEmittedByDataEntryUpdateHandler(EMPTY_IMAGE, newImage);
        expandResourceHandler.handleRequest(event, output, CONTEXT);
        var eventReference = parseHandlerResponse();
        assertThat(eventReference, is(equalTo(emptyEvent(eventReference.getTimestamp()))));
    }

    @Test
    void shouldEmitDeleteEventForPublicationStatusDeleted() throws IOException {
        var oldImage = randomPublication().copy()
                           .withIdentifier(SortableIdentifier.next())
                           .withDoi(null)
                           .withStatus(PublicationStatus.PUBLISHED).build();
        var newImage = oldImage.copy()
                           .withStatus(PublicationStatus.DELETED)
                           .build();
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, newImage);
        expandResourceHandler.handleRequest(request, output, CONTEXT);
        var eventReference = parseHandlerResponse();
        assertThat(eventReference.getTopic(), is(equalTo(EXPANDED_ENTRY_DELETE_EVENT_TOPIC)));
    }

    @Test
    @Disabled
    //TODO: implement this test as a test or a set of tests
    void shouldAlwaysEmitEventsForAllTypesOfDataEntries() {

    }

    private Publication createUpdatedVersionOfPublication(Publication oldImage) {
        return oldImage.copy().withModifiedDate(randomInstant(oldImage.getModifiedDate())).build();
    }

    private InputStream emulateEventEmittedByDataEntryUpdateHandler(Object oldImage, Object newImage)
        throws IOException {
        var blobUri = createSampleBlob(oldImage, newImage);
        var event = new EventReference(RESOURCE_UPDATE_EVENT_TOPIC, blobUri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(event);
    }

    private Publication createPublicationWithStatus(PublicationStatus status) {
        return randomPublication().copy().withStatus(status).build();
    }

    private URI createSampleBlob(Object oldImage, Object newImage) throws IOException {
        var oldImageResource = crateDataEntry(oldImage);
        var newImageResource = crateDataEntry(newImage);
        var dataEntryUpdateEvent =
            new DataEntryUpdateEvent(RESOURCE_UPDATE_EVENT_TOPIC, oldImageResource, newImageResource);
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        return s3Driver.insertFile(filePath, dataEntryUpdateEvent.toJsonString());
    }

    private Entity crateDataEntry(Object image) {

        if (image instanceof Publication) {
            return Resource.fromPublication((Publication) image);
        } else if (image instanceof DoiRequest) {
            return (DoiRequest) image;
        } else if (image instanceof Message) {
            return (Message) image;
        } else {
            return null;
        }
    }

    private void insertPublicationWithIdentifierAndAffiliationAsTheOneFoundInResources() {
        var publication = randomPublication().copy()
                              .withIdentifier(new SortableIdentifier(IDENTIFIER_IN_RESOURCE_FILE))
                              .withResourceOwner(
                                  new ResourceOwner(randomUsername(),
                                                    AFFILIATION_URI_FOUND_IN_FAKE_PERSON_API_RESPONSE))
                              .build();
        attempt(() -> resourceService.insertPreexistingPublication(publication)).orElseThrow();
    }

    private Username randomUsername() {
        return new Username(randomString());
    }

    private EventReference emptyEvent(Instant timestamp) {
        return new EventReference(EMPTY_EVENT_TOPIC, null, null, timestamp);
    }

    private DoiRequest doiRequestForDraftResource() {
        Publication publication = randomPublication().copy()
                                      .withStatus(DRAFT)
                                      .build();
        Resource resource = Resource.fromPublication(publication);
        return DoiRequest.newDoiRequestForResource(resource);
    }

    private ResourceExpansionService createFailingService() {
        return new ResourceExpansionService() {
            @Override
            public ExpandedDataEntry expandEntry(Entity dataEntry) {
                throw new RuntimeException(EXPECTED_ERROR_MESSAGE);
            }

            @Override
            public Set<URI> getOrganizationIds(Entity dataEntry) {
                return null;
            }

            @Override
            public ExpandedPerson enrichPerson(User username) {
                return null;
            }
        };
    }

    private EventReference parseHandlerResponse() throws JsonProcessingException {
        return objectMapper.readValue(output.toString(), EventReference.class);
    }
}
