package no.unit.nva.publication.events.handlers.persistence;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.persistence.ExpandedDataEntriesPersistenceHandler.EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.persistence.PersistedDocumentConsumptionAttributes.RESOURCES_INDEX;
import static no.unit.nva.publication.events.handlers.persistence.PersistedDocumentConsumptionAttributes.TICKETS_INDEX;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
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
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedGeneralSupportRequest;
import no.unit.nva.expansion.model.ExpandedPublishingRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.model.Publication;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ExpandedDataEntriesPersistenceHandlerTest extends ResourcesLocalTest {
    
    private static final String HELP_MESSAGE = String.format("%s should be compared for equality only as json "
                                                             + "objects", ExpandedResource.class.getSimpleName());
    private ExpandedDataEntriesPersistenceHandler handler;
    private S3Driver s3Reader;
    private S3Driver s3Writer;
    private URI eventUriInEventsBucket;
    private ByteArrayOutputStream output;
    private ResourceService resourceService;
    private TicketService ticketService;
    private ResourceExpansionService resourceExpansionService;
    
    @BeforeEach
    public void setup() {
        super.init();
        Clock clock = Clock.systemDefaultZone();
        resourceService = new ResourceService(client, clock);
        ticketService = new TicketService(client);
        resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, ticketService);
    }
    
    @BeforeEach
    public void init() {
        var eventsBucket = new FakeS3Client();
        var indexBucket = new FakeS3Client();
        s3Reader = new S3Driver(eventsBucket, "eventsBucket");
        s3Writer = new S3Driver(indexBucket, "indexBucket");
        handler = new ExpandedDataEntriesPersistenceHandler(s3Reader, s3Writer);
        
        output = new ByteArrayOutputStream();
    }
    
    @ParameterizedTest(name = "should emit event containing S3 URI to persisted expanded resource:{0}")
    @MethodSource("expandedEntriesTypeProvider")
    void shouldEmitEventContainingS3UriToPersistedExpandedResource(Class<?> entryType)
        throws IOException, ApiGatewayException {
        var entryUpdate = generateExpandedEntry(entryType);
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), entryUpdate.entry.toJsonString());
        EventReference outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getUri());
        assertThat(indexingEventPayload, is(not(emptyString())));
    }
    
    @ParameterizedTest(name = "should store entry containing the data referenced in the received event:{0}")
    @MethodSource("expandedEntriesTypeProvider")
    void shouldStoreEntryContainingTheDataReferencedInTheReceivedEvent(Class<?> entryType)
        throws IOException, ApiGatewayException {
        var update = generateExpandedEntry(entryType).entry;
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), update.toJsonString());
        EventReference outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getUri());
        PersistedDocument indexDocument = PersistedDocument.fromJsonString(indexingEventPayload);
        var expectedJson = (Object) JsonUtils.dtoObjectMapper.readTree(
            ((JsonSerializable) update).toJsonString());
        var actualJson = JsonUtils.dtoObjectMapper.readTree(indexDocument.getBody().toJsonString());
        assertThat(HELP_MESSAGE, actualJson, is(equalTo(expectedJson)));
    }
    
    @ParameterizedTest(name = "should store entry containing the general type (index name) of the persisted event")
    @MethodSource("expandedEntriesTypeProvider")
    void shouldStoreEntryContainingTheIndexNameForThePersistedEntry(Class<?> expandedEntryType)
        throws IOException, ApiGatewayException {
        
        var expectedPersistedEntry = generateExpandedEntry(expandedEntryType);
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()),
            expectedPersistedEntry.entry.toJsonString());
        EventReference outputEvent = sendEvent();
        String indexingEventPayload = s3Writer.readEvent(outputEvent.getUri());
        PersistedDocument indexDocument = PersistedDocument.fromJsonString(indexingEventPayload);
        assertThat(indexDocument.getConsumptionAttributes().getIndex(), is(equalTo(expectedPersistedEntry.index)));
    }
    
    private static Stream<Class<?>> expandedEntriesTypeProvider() {
        return TypeProvider.listSubTypes(ExpandedDataEntry.class);
    }
    
    private PersistedEntryWithExpectedType generateExpandedEntry(Class<?> expandedEntryType)
        throws JsonProcessingException, ApiGatewayException {
        if (ExpandedResource.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomResource(), RESOURCES_INDEX);
        } else if (ExpandedDoiRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomDoiRequest(), TICKETS_INDEX);
        } else if (ExpandedPublishingRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomPublishingRequest(), TICKETS_INDEX);
        } else if (ExpandedGeneralSupportRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomGeneralSupportRequest(), TICKETS_INDEX);
        }
        throw new RuntimeException();
    }
    
    private ExpandedDataEntry randomGeneralSupportRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var openingCaseObject =
            TicketEntry.requestNewTicket(publication, GeneralSupportRequest.class).persist(ticketService);
        return resourceExpansionService.expandEntry(openingCaseObject);
    }
    
    private ExpandedPublishingRequest randomPublishingRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var userInstance = UserInstance.fromPublication(publication);
        var publishingRequest = PublishingRequestCase
                                    .createOpeningCaseObject(userInstance, publication.getIdentifier())
                                    .persist(ticketService);
    
        return (ExpandedPublishingRequest) resourceExpansionService.expandEntry(publishingRequest);
    }
    
    private ExpandedResource randomResource() throws JsonProcessingException, ApiGatewayException {
        var resource = Resource.fromPublication(createPublicationWithoutDoi());
        return (ExpandedResource) resourceExpansionService.expandEntry(resource);
    }
    
    private Publication createPublicationWithoutDoi() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication().copy().withDoi(null).build();
        var persisted = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        return resourceService.getPublicationByIdentifier(persisted.getIdentifier());
    }
    
    private ExpandedDoiRequest randomDoiRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var doiRequest = DoiRequest.fromPublication(publication).persist(ticketService);
        return (ExpandedDoiRequest) resourceExpansionService.expandEntry(doiRequest);
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