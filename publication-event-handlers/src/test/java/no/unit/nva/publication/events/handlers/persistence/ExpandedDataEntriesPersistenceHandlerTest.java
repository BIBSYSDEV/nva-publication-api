package no.unit.nva.publication.events.handlers.persistence;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.publication.events.handlers.persistence.ExpandedDataEntriesPersistenceHandler.EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC;
import static no.unit.nva.publication.events.handlers.persistence.PersistedDocumentConsumptionAttributes.IMPORT_CANDIDATES_INDEX;
import static no.unit.nva.publication.events.handlers.persistence.PersistedDocumentConsumptionAttributes.RESOURCES_INDEX;
import static no.unit.nva.publication.events.handlers.persistence.PersistedDocumentConsumptionAttributes.TICKETS_INDEX;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.text.IsEmptyString.emptyString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedDoiRequest;
import no.unit.nva.expansion.model.ExpandedGeneralSupportRequest;
import no.unit.nva.expansion.model.ExpandedImportCandidate;
import no.unit.nva.expansion.model.ExpandedPublishingRequest;
import no.unit.nva.expansion.model.ExpandedResource;
import no.unit.nva.expansion.model.ExpandedUnpublishRequest;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.ResearchProject;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.funding.FundingBuilder;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatusFactory;
import no.unit.nva.publication.service.FakeSqsClient;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.testing.TypeProvider;
import no.unit.nva.publication.uriretriever.FakeUriResponse;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UnixPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
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
    private FakeUriRetriever uriRetriever;
    private FakeSqsClient fakeSqsClient;

    @BeforeEach
    public void setup() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        ticketService = getTicketService();
        fakeSqsClient = new FakeSqsClient();
        uriRetriever = FakeUriRetriever.newInstance();

        resourceExpansionService = new ResourceExpansionServiceImpl(resourceService, ticketService,
                                                                    uriRetriever, uriRetriever);
    }

    @BeforeEach
    @Override
    public void init() {
        var eventsBucket = new FakeS3Client();
        var indexBucket = new FakeS3Client();
        s3Reader = new S3Driver(eventsBucket, "eventsBucket");
        s3Writer = new S3Driver(indexBucket, "indexBucket");
        handler = new ExpandedDataEntriesPersistenceHandler(s3Reader, s3Writer, fakeSqsClient);
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

    @ParameterizedTest
    @DisplayName("should store entry containing the general type (index name) of the persisted event")
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

    @Test
    void shouldPersistRecoveryMessageForPublicationWhenSomethingGoesWrong() throws IOException, ApiGatewayException {
        final var expectedPersistedEntry = generateExpandedEntry(ExpandedResource.class);
        s3Writer = mock(S3Driver.class);
        when(s3Writer.insertFile(any(), (String) any())).thenThrow(new RuntimeException());
        handler = new ExpandedDataEntriesPersistenceHandler(s3Reader, s3Writer, fakeSqsClient);
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()),
                                                      expectedPersistedEntry.entry.toJsonString());
        sendEvent();
        var persistedRecoveryMessage = fakeSqsClient.getDeliveredMessages().getFirst();
        var messageAttributes = persistedRecoveryMessage.messageAttributes();
        assertThat(messageAttributes.get("id").stringValue(),
                   is(equalTo(expectedPersistedEntry.entry.identifyExpandedEntry().toString())));
        assertThat(messageAttributes.get("type").stringValue(),
                   is(equalTo("Resource")));
    }

    @Test
    void shouldPersistRecoveryMessageForTicketWhenSomethingGoesWrong() throws IOException, ApiGatewayException {
        final var expectedPersistedEntry = generateExpandedEntry(ExpandedPublishingRequest.class);
        s3Writer = mock(S3Driver.class);
        when(s3Writer.insertFile(any(), (String) any())).thenThrow(new RuntimeException());
        handler = new ExpandedDataEntriesPersistenceHandler(s3Reader, s3Writer, fakeSqsClient);
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()),
                                                      expectedPersistedEntry.entry.toJsonString());
        sendEvent();
        var persistedRecoveryMessage = fakeSqsClient.getDeliveredMessages().getFirst();
        var messageAttributes = persistedRecoveryMessage.messageAttributes();
        assertThat(messageAttributes.get("id").stringValue(),
                   is(equalTo(expectedPersistedEntry.entry.identifyExpandedEntry().toString())));
        assertThat(messageAttributes.get("type").stringValue(),
                   is(equalTo("Ticket")));
    }

    @Test
    void persistedUnpublishRequestEntryShouldBeDeserializable()
        throws IOException, ApiGatewayException {
        var entryUpdate = generateExpandedEntry(ExpandedUnpublishRequest.class);
        eventUriInEventsBucket = s3Reader.insertEvent(UnixPath.of(randomString()), entryUpdate.entry.toJsonString());
        var outputEvent = sendEvent();
        var persistedEvent = s3Writer.readEvent(outputEvent.getUri());
        var expandedEntryJson = JsonUtils.dtoObjectMapper.readTree(persistedEvent).at("/body");

        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(expandedEntryJson.toString(),
                                                                  ExpandedDataEntry.class));
    }

    private static Stream<Named<Class<?>>> expandedEntriesTypeProvider() {
        return TypeProvider.listSubTypes(ExpandedDataEntry.class);
    }

    private PersistedEntryWithExpectedType generateExpandedEntry(Class<?> expandedEntryType)
        throws JsonProcessingException, ApiGatewayException {
        if (ExpandedResource.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomResource(), RESOURCES_INDEX);
        } else if (ExpandedImportCandidate.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomExpandedImportCandidate(), IMPORT_CANDIDATES_INDEX);
        } else if (ExpandedDoiRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomDoiRequest(), TICKETS_INDEX);
        } else if (ExpandedPublishingRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomPublishingRequest(), TICKETS_INDEX);
        } else if (ExpandedGeneralSupportRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomGeneralSupportRequest(), TICKETS_INDEX);
        } else if (ExpandedUnpublishRequest.class.equals(expandedEntryType)) {
            return new PersistedEntryWithExpectedType(randomUnpublishRequest(), TICKETS_INDEX);
        }
        throw new RuntimeException();
    }

    private ImportCandidate randomImportCandidate() {
        return new ImportCandidate.Builder()
                   .withImportStatus(ImportStatusFactory.createNotImported())
                   .withEntityDescription(randomEntityDescription())
                   .withLink(randomUri())
                   .withIndexedDate(Instant.now())
                   .withPublishedDate(Instant.now())
                   .withHandle(randomUri())
                   .withModifiedDate(Instant.now())
                   .withCreatedDate(Instant.now())
                   .withPublisher(new Organization.Builder().withId(randomUri()).build())
                   .withSubjects(List.of(randomUri()))
                   .withIdentifier(SortableIdentifier.next())
                   .withRightsHolder(randomString())
                   .withProjects(List.of(new ResearchProject.Builder().withId(randomUri()).build()))
                   .withFundings(List.of(new FundingBuilder().withId(randomUri()).build()))
                   .withAdditionalIdentifiers(Set.of(new AdditionalIdentifier(randomString(), randomString())))
                   .withResourceOwner(new ResourceOwner(new Username(randomString()), randomUri()))
                   .withAssociatedArtifacts(List.of())
                   .build();
    }

    private EntityDescription randomEntityDescription() {
        return new EntityDescription.Builder()
                   .withPublicationDate(new PublicationDate.Builder().withYear("2020").build())
                   .withAbstract(randomString())
                   .withDescription(randomString())
                   .withContributors(List.of(randomContributor()))
                   .withMainTitle(randomString())
                   .build();
    }

    private Contributor randomContributor() {
        return new Contributor.Builder()
                   .withIdentity(new Identity.Builder().withName(randomString()).build())
                   .withRole(new RoleType(Role.ACTOR))
                   .build();
    }

    private ExpandedDataEntry randomExpandedImportCandidate() {
        return ExpandedImportCandidate.fromImportCandidate(randomImportCandidate(), uriRetriever);
    }

    private ExpandedDataEntry randomGeneralSupportRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var openingCaseObject =
            TicketEntry.requestNewTicket(publication, GeneralSupportRequest.class)
                .withOwner(UserInstance.fromPublication(publication).getUsername())
                .persistNewTicket(ticketService);
        return resourceExpansionService.expandEntry(openingCaseObject, false);
    }

    private ExpandedDataEntry randomUnpublishRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var unpublishRequest =
            TicketEntry.requestNewTicket(publication, UnpublishRequest.class).persistNewTicket(ticketService);
        return resourceExpansionService.expandEntry(unpublishRequest, false);
    }

    private ExpandedPublishingRequest randomPublishingRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var publishingRequest = PublishingRequestCase
                                                            .create(Resource.fromPublication(publication),
                                                                    UserInstance.fromPublication(publication),
                                                                    REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES)
                                    .persistNewTicket(ticketService);
        return (ExpandedPublishingRequest) resourceExpansionService.expandEntry(publishingRequest, false);
    }

    private ExpandedResource randomResource() throws JsonProcessingException, ApiGatewayException {
        var publication = createPublicationWithoutDoi();
        var resource = Resource.fromPublication(publication);
        return (ExpandedResource) resourceExpansionService.expandEntry(resource, false);
    }

    private Publication createPublicationWithoutDoi() throws ApiGatewayException {
        var publication = randomPublication(AcademicArticle.class).copy().withDoi(null).build();
        var persisted = Resource.fromPublication(publication)
                            .persistNew(resourceService, UserInstance.fromPublication(publication));
        FakeUriResponse.setupFakeForType(persisted, uriRetriever, resourceService);
        return resourceService.getPublicationByIdentifier(persisted.getIdentifier());
    }

    private ExpandedDoiRequest randomDoiRequest() throws ApiGatewayException, JsonProcessingException {
        var publication = createPublicationWithoutDoi();
        var doiRequest = DoiRequest.create(Resource.fromPublication(publication),
                                           UserInstance.fromPublication(publication))
                             .persistNewTicket(ticketService);
        return (ExpandedDoiRequest) resourceExpansionService.expandEntry(doiRequest, false);
    }

    private EventReference sendEvent() throws JsonProcessingException {
        EventReference eventReference =
            new EventReference(EXPANDED_ENTRY_PERSISTED_EVENT_TOPIC, eventUriInEventsBucket);
        var event = EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventReference);
        handler.handleRequest(event, output, null);
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