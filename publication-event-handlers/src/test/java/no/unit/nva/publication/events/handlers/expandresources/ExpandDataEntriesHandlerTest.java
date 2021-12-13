package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.expandresources.ExpandDataEntriesHandler.EMPTY_EVENT_TOPIC;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static nva.commons.core.ioutils.IoUtils.stringToStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Set;
import no.unit.nva.events.handlers.EventParser;
import no.unit.nva.events.models.AwsEventBridgeDetail;
import no.unit.nva.events.models.AwsEventBridgeEvent;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DoiRequest;
import no.unit.nva.publication.storage.model.Message;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.testing.http.FakeHttpClient;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.core.paths.UnixPath;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExpandDataEntriesHandlerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = mock(Context.class);
    public static final int SINGLE_EXPECTED_FILE = 0;
    public static final String EVENT_WITH_NEW_PUBLISHED_RESOURCE = stringFromResources(
        Path.of("expandResources/sample-event-old-is-draft-new-is-published.json"));
    public static final String EXPECTED_ERROR_MESSAGE = "expected error message";
    public static final String IDENTIFIER_IN_RESOURCE_FILE = "017ca2670694-37f2c1a7-0105-452c-b7b3-1d90a44a11c0";
    public static final Clock CLOCK = Clock.systemDefaultZone();
    private ByteArrayOutputStream output;
    private ExpandDataEntriesHandler expandResourceHandler;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;

    private static final URI AFFILIATION_URI_FOUND_IN_FAKE_PERSON_API_RESPONSE =
        URI.create("https://api.cristin.no/v2/units/194.63.10.0");
    private static final String PERSON_API_RESPONSE_WITH_UNIT =
        stringFromResources(Path.of("expandResources", "fake_person_api_response_with_unit.json"));
    private static final String INSTITUTION_PROXY_ORG_RESPONSE =
        stringFromResources(Path.of("expandResources", "cristin_org.json"));
    private static final String INSTITUTION_PROXY_PARENT_ORG_RESPONSE =
        stringFromResources(Path.of("expandResources", "cristin_parent_org.json"));
    private static final String INSTITUTION_PROXY_GRAND_PARENT_ORG_RESPONSE =
        stringFromResources(Path.of("expandResources", "cristin_grand_parent_org.json"));
    private ResourceService resourceService;

    @BeforeEach
    public void init() {
        super.init();
        this.output = new ByteArrayOutputStream();
        s3Client = new FakeS3Client();
        var httpClient = new FakeHttpClient<>(PERSON_API_RESPONSE_WITH_UNIT,
                                              INSTITUTION_PROXY_ORG_RESPONSE,
                                              INSTITUTION_PROXY_PARENT_ORG_RESPONSE,
                                              INSTITUTION_PROXY_GRAND_PARENT_ORG_RESPONSE);

        resourceService = new ResourceService(client, httpClient, CLOCK);
        insertPublicationWithIdentifierAndAffiliationAsTheOneFoundInResources();
        ResourceExpansionService resourceExpansionService =
            new ResourceExpansionServiceImpl(httpClient, resourceService);
        this.expandResourceHandler = new ExpandDataEntriesHandler(s3Client, resourceExpansionService);
        this.s3Driver = new S3Driver(s3Client, "ignoredForFakeS3Client");
    }

    private Publication insertPublicationWithIdentifierAndAffiliationAsTheOneFoundInResources() {
        var publication = PublicationGenerator.randomPublication().copy()
            .withIdentifier(new SortableIdentifier(IDENTIFIER_IN_RESOURCE_FILE))
            .withResourceOwner(new ResourceOwner(randomString(), AFFILIATION_URI_FOUND_IN_FAKE_PERSON_API_RESPONSE))
            .build();
        return attempt(() -> resourceService.insertPreexistingPublication(publication)).orElseThrow();
    }

    @Test
    void shouldSaveTheNewestResourceImageInS3WhenThereIsNewResourceImagePresentInTheEventAndIsNotDraftResource()
        throws JsonProcessingException {
        expandResourceHandler.handleRequest(sampleEvent(), output, CONTEXT);
        var allFiles = s3Driver.listAllFiles(UnixPath.ROOT_PATH);
        assertThat(allFiles.size(), is(equalTo(1)));
        var contents = s3Driver.getFile(allFiles.get(SINGLE_EXPECTED_FILE));
        var documentToIndex = objectMapper.readValue(contents, Publication.class);

        DataEntry actualResource = Resource.fromPublication(documentToIndex);
        DataEntry expectedImage = extractResourceUpdateFromEvent();
        assertThat(actualResource, is(equalTo(expectedImage)));
    }

    @Test
    void shouldEmitEventThatContainsTheEventPayloadS3Uri()
        throws JsonProcessingException {
        expandResourceHandler.handleRequest(sampleEvent(), output, CONTEXT);
        var updateEvent = parseEmittedEvent();
        var uriWithEventPayload = updateEvent.getUri();
        var actualResourceUpdate = fetchResourceUpdateFromS3(uriWithEventPayload);
        var expectedResourceUpdate = extractResourceUpdateFromEvent();
        assertThat(actualResourceUpdate, is(equalTo(expectedResourceUpdate)));
    }

    @Test
    void shouldLogFailingExpansionNotThrowExceptionAndEmitEmptyEvent() {
        TestAppender logs = LogUtils.getTestingAppenderForRootLogger();
        ResourceExpansionService failingService = createFailingService();
        expandResourceHandler = new ExpandDataEntriesHandler(s3Client, failingService);
        expandResourceHandler.handleRequest(sampleEvent(), output, CONTEXT);
        assertThat(logs.getMessages(), containsString(EXPECTED_ERROR_MESSAGE));
        assertThat(logs.getMessages(), containsString(IDENTIFIER_IN_RESOURCE_FILE));
    }

    @Test
    void shouldIgnoreAndNotCreateEnrichmentEventForDraftResources() throws JsonProcessingException {
        Publication publication = PublicationGenerator.randomPublication().copy()
            .withStatus(PublicationStatus.DRAFT)
            .build();
        Resource resource = Resource.fromPublication(publication);
        InputStream event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(resource);
        expandResourceHandler.handleRequest(event, output, CONTEXT);
        EventReference eventReference =
            objectMapper.readValue(output.toString(), EventReference.class);
        assertThat(eventReference, is(equalTo(emptyEvent())));
    }

    @Test
    void shouldIgnoreAndNotCreateEnrichmentEventForDoiRequestsOfDraftResources() throws JsonProcessingException {
        DoiRequest doiRequest = doiRequestForDraftResource();

        InputStream event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(doiRequest);
        expandResourceHandler.handleRequest(event, output, CONTEXT);
        EventReference eventReference =
            objectMapper.readValue(output.toString(), EventReference.class);
        assertThat(eventReference, is(equalTo(emptyEvent())));
    }

    @Test
    void shouldAlwaysEmitEventsForMessages() throws JsonProcessingException {
        Message someMessage = sampleMessage();
        InputStream event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(someMessage);
        expandResourceHandler.handleRequest(event, output, CONTEXT);
        EventReference eventReference =
            objectMapper.readValue(output.toString(), EventReference.class);
        assertThat(eventReference, is(equalTo(emptyEvent())));
    }

    private EventReference emptyEvent() {
        return new EventReference(EMPTY_EVENT_TOPIC, null);
    }

    private Message sampleMessage() {
        Publication publication = PublicationGenerator.randomPublication();
        UserInstance someUser = new UserInstance(randomString(), randomUri());
        Clock clock = Clock.systemDefaultZone();
        return Message.supportMessage(someUser, publication, randomString(), SortableIdentifier.next(), clock);
    }

    private DoiRequest doiRequestForDraftResource() {
        Publication publication = PublicationGenerator.randomPublication().copy()
            .withStatus(PublicationStatus.DRAFT)
            .build();
        Resource resource = Resource.fromPublication(publication);
        return DoiRequest.newDoiRequestForResource(resource);
    }

    private InputStream sampleEvent() {
        return stringToStream(EVENT_WITH_NEW_PUBLISHED_RESOURCE);
    }

    private ResourceExpansionService createFailingService() {
        return new ResourceExpansionService() {
            @Override
            public ExpandedDataEntry expandEntry(DataEntry dataEntry) {
                throw new RuntimeException(EXPECTED_ERROR_MESSAGE);
            }

            @Override
            public Set<URI> getOrganizationIds(DataEntry dataEntry) {
                return null;
            }
        };
    }

    private DataEntry fetchResourceUpdateFromS3(URI uriWithEventPayload) throws JsonProcessingException {
        var resourceUpdateString = s3Driver.getFile(new UriWrapper(uriWithEventPayload).toS3bucketPath());
        Publication publication =
            objectMapper.readValue(resourceUpdateString, Publication.class);
        return Resource.fromPublication(publication);
    }

    private EventReference parseEmittedEvent() throws JsonProcessingException {
        return objectMapper.readValue(output.toString(), EventReference.class);
    }

    private DataEntry extractResourceUpdateFromEvent() {
        var event =
            parseEvent();
        return event.getDetail().getResponsePayload().getNewData();
    }

    @SuppressWarnings("unchecked")
    private AwsEventBridgeEvent<AwsEventBridgeDetail<DataEntryUpdateEvent>> parseEvent() {
        return (AwsEventBridgeEvent<AwsEventBridgeDetail<DataEntryUpdateEvent>>)
                   newEventParser()
                       .parse(AwsEventBridgeDetail.class, DataEntryUpdateEvent.class);
    }

    private EventParser<AwsEventBridgeDetail<DataEntryUpdateEvent>> newEventParser() {
        return new EventParser<>(EVENT_WITH_NEW_PUBLISHED_RESOURCE, objectMapper);
    }
}
