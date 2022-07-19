package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.expandresources.ExpandedDataEntriesPersistenceHandler.EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocumentConsumptionAttributes.DOI_REQUESTS_INDEX;
import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocumentConsumptionAttributes.MESSAGES_INDEX;
import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocumentConsumptionAttributes.PUBLISHING_REQUESTS_INDEX;
import static no.unit.nva.publication.events.handlers.expandresources.PersistedDocumentConsumptionAttributes.RESOURCES_INDEX;
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
import no.unit.nva.expansion.model.ExpandedPublishingRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedResourceConversation;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.DoiRequestService;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.PublishingRequestService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.storage.model.MessageType;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.testing.SubTypeProvider;
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
    private DoiRequestService doiRequestService;
    private PublishingRequestService publishingRequestService;
    private MessageService messageService;
    private ResourceExpansionService resourceExpansionService;
    
    @BeforeEach
    public void setup() {
        super.init();
        Clock clock = Clock.systemDefaultZone();
        resourceService = new ResourceService(client, clock);
        doiRequestService = new DoiRequestService(client, clock);
        messageService = new MessageService(client, clock);
        publishingRequestService = new PublishingRequestService(client, clock);
        resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, messageService, doiRequestService,
            publishingRequestService);
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
        return SubTypeProvider.dataEntryTypeProvider(ExpandedDataEntry.class);
    }
    
    private PersistedEntryWithExpectedType generateExpandedEntry(Class<?> expandedEntryType)
        throws JsonProcessingException, ApiGatewayException {
        if (ExpandedResource.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomResource(), RESOURCES_INDEX);
        } else if (ExpandedDoiRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomDoiRequest(), DOI_REQUESTS_INDEX);
        } else if (ExpandedResourceConversation.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomResourceConversation(), MESSAGES_INDEX);
        } else if (ExpandedPublishingRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomPublishingRequest(), PUBLISHING_REQUESTS_INDEX);
        }
        throw new RuntimeException();
    }
    
    private ExpandedPublishingRequest randomPublishingRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createResource().toPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var openingCaseObject =
            PublishingRequestCase.createOpeningCaseObject(userInstance, publication.getIdentifier());
        var publishingRequest = publishingRequestService.createPublishingRequest(openingCaseObject);
        return (ExpandedPublishingRequest) resourceExpansionService.expandEntry(publishingRequest);
    }
    
    private ExpandedResource randomResource() throws JsonProcessingException, ApiGatewayException {
        var resource = createResource();
        return (ExpandedResource) resourceExpansionService.expandEntry(resource);
    }
    
    private Resource createResource() throws ApiGatewayException {
        var publication = PublicationGenerator.randomPublication();
        var persisted = resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        return resourceService.getResourceByIdentifier(persisted.getIdentifier());
    }
    
    private ExpandedDoiRequest randomDoiRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createResource().toPublication();
        var doiRequestIdentifier = doiRequestService.createDoiRequest(publication);
        var doiRequest =
            doiRequestService.getDoiRequest(UserInstance.fromPublication(publication), doiRequestIdentifier);
        return (ExpandedDoiRequest) resourceExpansionService.expandEntry(doiRequest);
    }
    
    private ExpandedResourceConversation randomResourceConversation()
        throws ApiGatewayException, JsonProcessingException {
        var publication = createResource().toPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var messageIdentifier = messageService.createMessage(userInstance, publication, randomString(),
            MessageType.SUPPORT);
        var message = messageService.getMessage(UserInstance.fromPublication(publication), messageIdentifier);
        return (ExpandedResourceConversation) resourceExpansionService.expandEntry(message);
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