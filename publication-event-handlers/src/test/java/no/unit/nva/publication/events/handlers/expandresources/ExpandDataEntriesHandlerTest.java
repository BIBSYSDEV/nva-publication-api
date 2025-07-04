package no.unit.nva.publication.events.handlers.expandresources;

import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.events.handlers.PublicationEventsConfig.objectMapper;
import static no.unit.nva.s3.S3Driver.GZIP_ENDING;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.OperationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.ResourceExpansionServiceImpl;
import no.unit.nva.expansion.model.ExpandedDataEntry;
import no.unit.nva.expansion.model.ExpandedMessage;
import no.unit.nva.expansion.model.ExpandedOrganization;
import no.unit.nva.expansion.model.ExpandedPerson;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.model.testing.PublicationGenerator;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.events.handlers.persistence.PersistedDocument;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.FakeSqsClient;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.MessageService;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.uriretriever.FakeUriResponse;
import no.unit.nva.publication.uriretriever.FakeUriRetriever;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

class ExpandDataEntriesHandlerTest extends ResourcesLocalTest {

    public static final Context CONTEXT = null;
    public static final String EXPECTED_ERROR_MESSAGE = "expected error message";
    private static final String EMPTY_EVENT_TOPIC = "Event.Empty";
    public static final String IDENTIFIER_IN_RESOURCE_FILE = "017ca2670694-37f2c1a7-0105-452c-b7b3-1d90a44a11c0";
    public static final Publication DELETED_RESOURCE = null;
    public static final Object EMPTY_IMAGE = null;
    private static final URI AFFILIATION_URI_FOUND_IN_FAKE_PERSON_API_RESPONSE =
        URI.create("https://api.cristin.no/v2/units/194.63.10.0");
    private static final String IGNORED = "ignored";
    private ByteArrayOutputStream output;
    private ExpandDataEntriesHandler expandResourceHandler;
    private S3Driver s3Driver;
    private FakeS3Client s3Client;
    private FakeSqsClient sqsClient;
    private ResourceService resourceService;
    private TicketService ticketService;
    private MessageService messageService;
    private FakeUriRetriever fakeUriRetriever;

    @Override
    @BeforeEach
    public void init() {
        super.init();
        this.output = new ByteArrayOutputStream();
        s3Client = new FakeS3Client();
        resourceService = getResourceService(client);
        sqsClient = new FakeSqsClient();
        ticketService = getTicketService();
        messageService = getMessageService();

        insertPublicationWithIdentifierAndAffiliationAsTheOneFoundInResources();

        fakeUriRetriever = FakeUriRetriever.newInstance();
        ResourceExpansionService resourceExpansionService =
            new ResourceExpansionServiceImpl(resourceService, ticketService, fakeUriRetriever, fakeUriRetriever,
                                             sqsClient);

        this.expandResourceHandler = new ExpandDataEntriesHandler(sqsClient, s3Client,
                                                                  resourceExpansionService);
        this.s3Driver = new S3Driver(s3Client, "ignoredForFakeS3Client");
    }

    @ParameterizedTest
    @EnumSource(
        value = PublicationStatus.class,
        mode = INCLUDE,
        names = {"PUBLISHED", "PUBLISHED_METADATA", "UNPUBLISHED", "DELETED", "DRAFT"})
    void shouldProduceAnExpandedDataEntryWhenInputHasNewImage(PublicationStatus status)
        throws IOException {
        var oldImage = createPublicationWithStatus(status);
        var newImage = createUpdatedVersionOfPublication(oldImage);
        var publication = resourceService.insertPreexistingPublication(newImage);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, publication);
        expandResourceHandler.handleRequest(request, output, CONTEXT);
        var persistedResource = s3Driver.getFile(
            UnixPath.of("resources", publication.getIdentifier().toString() + GZIP_ENDING));
        var persistedDocument = JsonUtils.dtoObjectMapper.readValue(persistedResource, PersistedDocument.class);
        assertThat(persistedDocument.getBody().identifyExpandedEntry(), is(equalTo(publication.getIdentifier())));
    }

    @ParameterizedTest
    @EnumSource(
        value = PublicationStatus.class,
        mode = EXCLUDE,
        names = {"PUBLISHED", "PUBLISHED_METADATA", "UNPUBLISHED", "DELETED", "DRAFT"})
    void shouldNotProduceEntryWhenNotPublishedOrDeletedEntry(PublicationStatus status)
        throws IOException {
        var oldImage = createPublicationWithStatus(status);
        var newImage = createUpdatedVersionOfPublication(oldImage);
        var publication = resourceService.insertPreexistingPublication(newImage);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, publication);
        expandResourceHandler.handleRequest(request, output, CONTEXT);
        var response = parseHandlerResponse();
        assertThat(response, is(equalTo(emptyEvent(response.getTimestamp()))));
    }

    @Test
    void shouldNotProduceAnExpandedDataEntryWhenInputHasNoNewImage() throws IOException {
        var oldImage = createPublicationWithStatus(PUBLISHED);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage,
                                                                  DELETED_RESOURCE);
        expandResourceHandler.handleRequest(request, output, CONTEXT);
        var response = parseHandlerResponse();
        assertThat(response, is(equalTo(emptyEvent(response.getTimestamp()))));
    }

    @Test
    void shouldThrowExceptionWhenExpansionFailing() throws IOException {
        var oldImage = createPublicationWithStatus(PUBLISHED);
        var newImage = createUpdatedVersionOfPublication(oldImage);
        var publication = resourceService.insertPreexistingPublication(newImage);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, publication);

        var logger = LogUtils.getTestingAppenderForRootLogger();

        expandResourceHandler = new ExpandDataEntriesHandler(sqsClient, s3Client, createFailingService());
        expandResourceHandler.handleRequest(request, output, CONTEXT);

        assertThat(logger.getMessages(), containsString("DateEntry has been sent to recovery queue"));
    }

    @Test
    void shouldPersistRecoveryMessageWhenExpansionHasFailed() throws IOException {
        var oldImage = createPublicationWithStatus(PUBLISHED);
        var newImage = createUpdatedVersionOfPublication(oldImage);
        var publication = resourceService.insertPreexistingPublication(newImage);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var request = emulateEventEmittedByDataEntryUpdateHandler(oldImage, publication);

        var sqsClient = new FakeSqsClient();
        expandResourceHandler = new ExpandDataEntriesHandler(sqsClient, s3Client, createFailingService());
        expandResourceHandler.handleRequest(request, output, CONTEXT);

        var persistedRecoveryMessage = sqsClient.getDeliveredMessages().getFirst();
        var messageAttributes = persistedRecoveryMessage.messageAttributes();
        assertThat(messageAttributes.get("id").stringValue(), is(equalTo(oldImage.getIdentifier().toString())));
    }

    @Test
    void shouldPersistRecoveryMessageWhenExpansionHasFailedAndOldImageIsNotPresent()
        throws IOException {
        var newImage = createPublicationWithStatus(PUBLISHED);
        var publication = resourceService.insertPreexistingPublication(newImage);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var request = emulateEventEmittedByDataEntryUpdateHandler(null, publication);

        var sqsClient = new FakeSqsClient();
        expandResourceHandler = new ExpandDataEntriesHandler(sqsClient, s3Client, createFailingService());
        expandResourceHandler.handleRequest(request, output, CONTEXT);

        var persistedRecoveryMessage = sqsClient.getDeliveredMessages().getFirst();
        var messageAttributes = persistedRecoveryMessage.messageAttributes();
        assertThat(messageAttributes.get("id").stringValue(), is(equalTo(publication.getIdentifier().toString())));
    }

    @Test
    void shouldPersistRecoveryMessageForPublicationWhenBadResponseFromExternalApi()
        throws IOException {
        var newImage = createPublicationWithStatus(PUBLISHED);
        var publication = resourceService.insertPreexistingPublication(newImage);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var request = emulateEventEmittedByDataEntryUpdateHandler(null, publication);

        var resourceExpansionService =
            new ResourceExpansionServiceImpl(resourceService, getTicketService(),
                                             uriRetrieverThrowingException(), uriRetrieverThrowingException(),
                                             sqsClient);
        this.expandResourceHandler = new ExpandDataEntriesHandler(sqsClient, s3Client,
                                                                  resourceExpansionService);
        expandResourceHandler.handleRequest(request, output, CONTEXT);

        var persistedRecoveryMessage = sqsClient.getDeliveredMessages().getFirst();
        var messageAttributes = persistedRecoveryMessage.messageAttributes();

        assertThat(messageAttributes.get("id").stringValue(), is(equalTo(publication.getIdentifier().toString())));
        assertThat(messageAttributes.get("type").stringValue(), is(equalTo("Resource")));
    }

    @Test
    void shouldPersistRecoveryMessageForTicketWhenBadResponseFromExternalApi() throws IOException {
        var publication = createPublicationWithStatus(PUBLISHED);
        var resource = resourceService.insertPreexistingPublication(publication);
        FakeUriResponse.setupFakeForType(resource, fakeUriRetriever, resourceService, false);
        var ticket = TicketEntry.requestNewTicket(resource, DoiRequest.class);
        var request = emulateEventEmittedByDataEntryUpdateHandler(null, ticket);

        var resourceExpansionService =
            new ResourceExpansionServiceImpl(resourceService, getTicketService(),
                                             uriRetrieverThrowingException(), uriRetrieverThrowingException(),
                                             sqsClient);
        this.expandResourceHandler = new ExpandDataEntriesHandler(sqsClient, s3Client,
                                                                  resourceExpansionService);
        expandResourceHandler.handleRequest(request, output, CONTEXT);

        var persistedRecoveryMessage = sqsClient.getDeliveredMessages().getFirst();
        var messageAttributes = persistedRecoveryMessage.messageAttributes();

        assertThat(messageAttributes.get("id").stringValue(), is(equalTo(ticket.getIdentifier().toString())));
        assertThat(messageAttributes.get("type").stringValue(), is(equalTo("Ticket")));
    }

    @Test
    void shouldPersistRecoveryMessageForMessageWhenBadResponseFromExternalApi() throws IOException,
                                                                                       ApiGatewayException {
        var publication = createPublicationWithStatus(PUBLISHED);
        var persistedPublication =
            resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, false);
        var ticket = TicketEntry.requestNewTicket(persistedPublication, GeneralSupportRequest.class)
                         .withOwner(UserInstance.fromPublication(persistedPublication).getUsername())
                         .persistNewTicket(ticketService);
        var message = messageService.createMessage(ticket, UserInstance.fromTicket(ticket), randomString());
        var request = emulateEventEmittedByDataEntryUpdateHandler(null, message);

        var resourceExpansionService =
            new ResourceExpansionServiceImpl(resourceService, getTicketService(),
                                             uriRetrieverThrowingException(), uriRetrieverThrowingException(),
                                             sqsClient);
        this.expandResourceHandler = new ExpandDataEntriesHandler(sqsClient, s3Client,
                                                                  resourceExpansionService);
        expandResourceHandler.handleRequest(request, output, CONTEXT);

        var persistedRecoveryMessage = sqsClient.getDeliveredMessages().getFirst();
        var messageAttributes = persistedRecoveryMessage.messageAttributes();

        assertThat(messageAttributes.get("id").stringValue(), is(equalTo(message.getIdentifier().toString())));
        assertThat(messageAttributes.get("type").stringValue(), is(equalTo("Message")));
    }

    @Test
    void shouldExpandTicketOnMessageInsertOrUpdate() throws ApiGatewayException, IOException {
        var publication = createPublicationWithStatus(PUBLISHED);
        var persistedPublication =
            resourceService.createPublication(UserInstance.fromPublication(publication), publication);
        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, false);
        var ticket = TicketEntry.requestNewTicket(persistedPublication, GeneralSupportRequest.class)
                         .withOwner(UserInstance.fromPublication(persistedPublication).getUsername())
                         .persistNewTicket(ticketService);
        var message = Message.create(ticket, UserInstance.fromTicket(ticket), randomString());
        var request = emulateEventEmittedByDataEntryUpdateHandler(null, message);

        expandResourceHandler.handleRequest(request, output, CONTEXT);

        var persistedResource = s3Driver.getFile(
            UnixPath.of("tickets", ticket.getIdentifier().toString() + GZIP_ENDING));
        var persistedDocument = JsonUtils.dtoObjectMapper.readValue(persistedResource, PersistedDocument.class);
        assertThat(persistedDocument.getBody().identifyExpandedEntry(), is(equalTo(ticket.getIdentifier())));
    }

    private static UriRetriever uriRetrieverThrowingException() {
        var mockUriRetriever = mock(UriRetriever.class);
        when(mockUriRetriever.getRawContent(any(), any())).thenThrow(new RuntimeException());
        return mockUriRetriever;
    }

    @Test
    void shouldCreateEnrichmentEventForMinimalisticInitialDrafts() throws IOException {
        var newImage = createMinimalisticDraft();
        var publication = resourceService.insertPreexistingPublication(newImage);
        FakeUriResponse.setupFakeForType(publication, fakeUriRetriever, resourceService, false);
        var request = emulateEventEmittedByDataEntryUpdateHandler(null, publication);

        expandResourceHandler.handleRequest(request, output, CONTEXT);

        var persistedResource = s3Driver.getFile(
            UnixPath.of("resources", publication.getIdentifier().toString() + GZIP_ENDING));
        var persistedDocument = JsonUtils.dtoObjectMapper.readValue(persistedResource, PersistedDocument.class);
        assertThat(persistedDocument.getBody().identifyExpandedEntry(), is(equalTo(publication.getIdentifier())));
    }

    private Publication createMinimalisticDraft() {
        var publication = createPublicationWithStatus(DRAFT);

        publication.setAssociatedArtifacts(AssociatedArtifactList.empty());
        publication.setEntityDescription(null);
        publication.setDoi(null);
        publication.setAdditionalIdentifiers(Collections.emptySet());
        publication.setFundings(Collections.emptyList());
        publication.setDuplicateOf(null);
        publication.setHandle(null);
        publication.setCuratingInstitutions(Collections.emptySet());
        publication.setImportDetails(Collections.emptyList());
        publication.setIndexedDate(null);
        publication.setLink(null);
        publication.setProjects(Collections.emptyList());
        publication.setRightsHolder(null);
        publication.setSubjects(Collections.emptyList());
        publication.setPublicationNotes(Collections.emptyList());

        return publication;
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
    void shouldExpandResourceOnFileEntryChange() throws IOException {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        publication.setStatus(PublicationStatus.PUBLISHED);
        var persistedPublication = resourceService.insertPreexistingPublication(publication);

        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, false);

        var openFile = extractOpenFile(publication);
        var newImage = fileEntryUpdate(openFile.orElseThrow(),
                                       publication.getIdentifier(),
                                       UserInstance.fromPublication(publication));
        var event = emulateEventEmittedByDataEntryUpdateHandler(EMPTY_IMAGE, newImage);
        expandResourceHandler.handleRequest(event, output, CONTEXT);

        var persistedResource = s3Driver.getFile(
            UnixPath.of("resources", publication.getIdentifier().toString() + GZIP_ENDING));
        var persistedDocument = JsonUtils.dtoObjectMapper.readValue(persistedResource, PersistedDocument.class);
        assertThat(persistedDocument.getBody().identifyExpandedEntry(), is(equalTo(publication.getIdentifier())));
    }

    @Test
    void shouldExpandResourceOnFileEntryDeletion() throws IOException {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        publication.setStatus(PublicationStatus.PUBLISHED);
        var persistedPublication = resourceService.insertPreexistingPublication(publication);

        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, false);

        var openFile = extractOpenFile(publication);
        var oldImage = fileEntryUpdate(openFile.orElseThrow(),
                                       publication.getIdentifier(),
                                       UserInstance.fromPublication(publication));
        var event = emulateEventEmittedByDataEntryUpdateHandler(oldImage, EMPTY_IMAGE);
        expandResourceHandler.handleRequest(event, output, CONTEXT);

        var persistedResource = s3Driver.getFile(
            UnixPath.of("resources", publication.getIdentifier().toString() + GZIP_ENDING));
        var persistedDocument = JsonUtils.dtoObjectMapper.readValue(persistedResource, PersistedDocument.class);
        assertThat(persistedDocument.getBody().identifyExpandedEntry(), is(equalTo(publication.getIdentifier())));
    }

    @Test
    void shouldExpandResourceOnFileEntryInsertion() throws IOException {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        publication.setStatus(PublicationStatus.PUBLISHED);
        var persistedPublication = resourceService.insertPreexistingPublication(publication);

        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, false);

        var openFile = extractOpenFile(publication);
        var image = fileEntryUpdate(openFile.orElseThrow(),
                                    publication.getIdentifier(),
                                    UserInstance.fromPublication(publication));
        var event = emulateEventEmittedByDataEntryUpdateHandler(EMPTY_IMAGE, image);
        expandResourceHandler.handleRequest(event, output, CONTEXT);

        var persistedResource = s3Driver.getFile(
            UnixPath.of("resources", publication.getIdentifier().toString() + GZIP_ENDING));
        var persistedDocument = JsonUtils.dtoObjectMapper.readValue(persistedResource, PersistedDocument.class);
        assertThat(persistedDocument.getBody().identifyExpandedEntry(), is(equalTo(publication.getIdentifier())));
    }

    @Test
    void shouldExpandResourceOnFileEntryModification() throws IOException {
        var publication = PublicationGenerator.randomPublication(AcademicArticle.class);
        publication.setStatus(PublicationStatus.PUBLISHED);
        var persistedPublication = resourceService.insertPreexistingPublication(publication);

        FakeUriResponse.setupFakeForType(persistedPublication, fakeUriRetriever, resourceService, false);

        var openFile = extractOpenFile(publication);
        var image = fileEntryUpdate(openFile.orElseThrow(),
                                    publication.getIdentifier(),
                                    UserInstance.fromPublication(publication));
        var event = emulateEventEmittedByDataEntryUpdateHandler(image, image);
        expandResourceHandler.handleRequest(event, output, CONTEXT);

        var persistedResource = s3Driver.getFile(
            UnixPath.of("resources", publication.getIdentifier().toString() + GZIP_ENDING));
        var persistedDocument = JsonUtils.dtoObjectMapper.readValue(persistedResource, PersistedDocument.class);
        assertThat(persistedDocument.getBody().identifyExpandedEntry(), is(equalTo(publication.getIdentifier())));
    }

    @Test
    void shouldNotFailIfResourceNoLongerExistOnDeletedFileEntry() throws IOException {
        var publication = PublicationGenerator.randomPublication();
        publication.setStatus(PublicationStatus.PUBLISHED);

        var openFile = extractOpenFile(publication);
        var oldImage = fileEntryUpdate(openFile.orElseThrow(),
                                       publication.getIdentifier(),
                                       UserInstance.fromPublication(publication));
        var event = emulateEventEmittedByDataEntryUpdateHandler(oldImage, EMPTY_IMAGE);

        assertDoesNotThrow(() -> expandResourceHandler.handleRequest(event, output, CONTEXT));

        assertThrows(NoSuchKeyException.class, () -> s3Driver.getFile(
            UnixPath.of("resources", publication.getIdentifier().toString() + GZIP_ENDING)));
    }

    private static Optional<OpenFile> extractOpenFile(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(OpenFile.class::isInstance)
                   .map(OpenFile.class::cast)
                   .findFirst();
    }

    private FileEntry fileEntryUpdate(OpenFile openFile,
                                      SortableIdentifier resourceIdentifier,
                                      UserInstance userInstance) {
        var file = File.builder()
                       .withEmbargoDate(openFile.getEmbargoDate().orElse(null))
                       .withIdentifier(openFile.getIdentifier())
                       .withLicense(openFile.getLicense())
                       .withLegalNote("The new legal note!")
                       .withName(openFile.getName())
                       .withMimeType(openFile.getMimeType())
                       .withSize(openFile.getSize())
                       .withPublisherVersion(openFile.getPublisherVersion())
                       .withUploadDetails(openFile.getUploadDetails())
                       .withRightsRetentionStrategy(openFile.getRightsRetentionStrategy())
                       .buildOpenFile();
        return FileEntry.create(file, resourceIdentifier, userInstance);
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
        var event = new EventReference(IGNORED, blobUri);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(event);
    }

    private Publication createPublicationWithStatus(PublicationStatus status) {
        return randomPublication(AcademicArticle.class).copy().withStatus(status).build();
    }

    private URI createSampleBlob(Object oldImage, Object newImage) throws IOException {
        var oldImageResource = crateDataEntry(oldImage);
        var newImageResource = crateDataEntry(newImage);
        var dataEntryUpdateEvent =
            new DataEntryUpdateEvent(OperationType.MODIFY.toString(), oldImageResource, newImageResource);
        var filePath = UnixPath.of(UUID.randomUUID().toString());
        return s3Driver.insertFile(filePath, dataEntryUpdateEvent.toJsonString());
    }

    private Entity crateDataEntry(Object image) {

        return switch (image) {
            case Publication publication -> Resource.fromPublication(publication);
            case DoiRequest doiRequest -> doiRequest;
            case Message message -> message;
            case FileEntry fileEntry -> fileEntry;
            case null, default -> null;
        };
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
        var publication = randomPublication().copy()
                              .withStatus(DRAFT)
                              .build();
        var resource = Resource.fromPublication(publication);
        return DoiRequest.create(resource, UserInstance.fromPublication(publication));
    }

    private ResourceExpansionService createFailingService() {
        return new ResourceExpansionService() {
            @Override
            public Optional<ExpandedDataEntry> expandEntry(Entity dataEntry, boolean ignored) {
                throw new RuntimeException(EXPECTED_ERROR_MESSAGE);
            }

            @Override
            public ExpandedOrganization getOrganization(Entity dataEntry) {
                return null;
            }

            @Override
            public ExpandedPerson expandPerson(User username) {
                return null;
            }

            @Override
            public ExpandedMessage expandMessage(Message messages) {
                return null;
            }
        };
    }

    private EventReference parseHandlerResponse() throws JsonProcessingException {
        return objectMapper.readValue(output.toString(), EventReference.class);
    }
}
