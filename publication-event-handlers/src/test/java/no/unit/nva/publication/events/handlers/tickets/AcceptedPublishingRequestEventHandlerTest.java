package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomUnpublishedFile;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.AdministrativeAgreement;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PublishedFile;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FileForApproval;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import no.unit.nva.s3.S3Driver;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.stubs.FakeS3Client;
import no.unit.nva.testutils.EventBridgeEventBuilder;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UnixPath;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

class AcceptedPublishingRequestEventHandlerTest extends ResourcesLocalTest {
    
    private static final Context CONTEXT = new FakeContext();
    public static final String RESOURCE_LACKS_REQUIRED_DATA = "Resource does not have required data to be "
                                                              + "published: ";
    private static final Username USERNAME = new Username(randomString());
    private ResourceService resourceService;
    private TicketService ticketService;
    private AcceptedPublishingRequestEventHandler handler;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;
    
    @BeforeEach
    public void setup() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        this.ticketService = getTicketService();
        var s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, randomString());
        handler = new AcceptedPublishingRequestEventHandler(resourceService, ticketService, s3Client);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldDoNothingWhenEventHasNoEffectiveChanges() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var publishingRequest = pendingPublishingRequest(publication).complete(publication, USERNAME);
        var event = createEvent(publishingRequest, publishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var notUpdatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(publication.getModifiedDate(), is(equalTo(notUpdatedPublication.getModifiedDate())));
        assertThat(notUpdatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldPublishFilesWhenPublicationIsAlreadyPublished() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication, USERNAME)
                                            .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }
    
    @Test
    void shouldPublishPublicationWhenPublishingRequestIsApproved() throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublication(PublicationStatus.PUBLISHED, resourceService);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication, USERNAME)
                                            .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @ParameterizedTest
    @EnumSource(value = PublishingWorkflow.class,
        names = {"REGISTRATOR_PUBLISHES_METADATA_ONLY",
            "REGISTRATOR_PUBLISHES_METADATA_AND_FILES",
            "REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES"},
        mode = Mode.INCLUDE)
    void shouldPublishFilesWhenPublishingRequestIsApproved(PublishingWorkflow value) throws ApiGatewayException,
                                                                                         IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithUnpublishedFiles(PublicationStatus.PUBLISHED,
                                                                                         resourceService);
        var pendingPublishingRequest =
            (PublishingRequestCase) persistPublishingRequestContainingExistingUnpublishedFiles(publication);
        pendingPublishingRequest.setWorkflow(value);
        var approvedPublishingRequest = pendingPublishingRequest.approveFiles()
                                            .complete(publication, USERNAME)
                                            .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(getAssociatedFiles(updatedPublication), everyItem(instanceOf(PublishedFile.class)));
    }

    @Test
    void shouldNotPublishAdministrativeAgreementWhenPublishingRequestIsApproved()
        throws ApiGatewayException, IOException {
        var publication = TicketTestUtils.createPersistedPublicationWithAdministrativeAgreement(resourceService);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication, USERNAME).persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(updatedPublication.getAssociatedArtifacts().getFirst(),
                   is(instanceOf(AdministrativeAgreement.class)));
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldCreateDoiRequestTicketWhenPublicationWithDraftDoiIsPublished()
        throws IOException, ApiGatewayException {
        var publication = createDraftPublicationWithDoi();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication, USERNAME).persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket = ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                                   publication.getIdentifier(),
                                                                   DoiRequest.class).orElseThrow();

        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(ticket.getStatus(), is(equalTo(TicketStatus.PENDING)));
        assertThat(ticket.getOwnerAffiliation(), is(equalTo(publication.getResourceOwner().getOwnerAffiliation())));
    }

    @Test
    void shouldNotCreateNewDoiRequestTicketWhenTicketAlreadyExists()
        throws ApiGatewayException, IOException {
        var publication = createDraftPublicationWithDoi();
        final var existingTicket = createDoiRequestTicket(publication);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication, USERNAME)
                                            .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var actualTicket = ticketService.fetchTicketByResourceIdentifier(publication.getPublisher().getId(),
                                                                   publication.getIdentifier(),
                                                                   DoiRequest.class).orElseThrow();
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(existingTicket.getIdentifier(), is(equalTo(actualTicket.getIdentifier())));
        assertThat(actualTicket.getResourceStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldNotPublishPublicationWhenPublishingRequestIsStillNotApproved() throws ApiGatewayException, IOException {
        var publication = createPublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var stillPendingPublishingRequest = modifyPublishingRequest(pendingPublishingRequest);
        var event = createEvent(pendingPublishingRequest, stillPendingPublishingRequest);
        
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
    }
    
    @Test
    void shouldThrowRuntimeExceptionAndLogExceptionMessageWhenPublishingPublicationFails()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishablePublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication, USERNAME)
                                            .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        var logger = LogUtils.getTestingAppenderForRootLogger();
        
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, outputStream, CONTEXT));

        assertThat(logger.getMessages(), containsString(RESOURCE_LACKS_REQUIRED_DATA));
    }

    @Test
    void shouldThrowRuntimeExceptionAndLogExceptionMessageWhenUpdatingPublicationFails()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishablePublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication, USERNAME)
                                            .persistNewTicket(ticketService);
        var handlerThrowingException = handlerWithResourceServiceThrowingExceptionWhenUpdatingPublication();
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        var logger = LogUtils.getTestingAppenderForRootLogger();

        assertThrows(RuntimeException.class, () -> handlerThrowingException.handleRequest(event, outputStream, CONTEXT));

        assertThat(logger.getMessages(), containsString("Could not update publication"));
    }

    @Test
    void shouldThrowRuntimeExceptionAndLogExceptionMessageWhenFetchingPublishingRequestFails()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishablePublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest = pendingPublishingRequest.complete(publication, USERNAME)
                                            .persistNewTicket(ticketService);
        var handlerThrowingException = handlerWithResourceServiceThrowingExceptionWhenFetchingTicket();
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        var logger = LogUtils.getTestingAppenderForRootLogger();

        assertThrows(RuntimeException.class, () -> handlerThrowingException.handleRequest(event, outputStream, CONTEXT));

        assertThat(logger.getMessages(), containsString("Could not fetch PublishingRequest"));
    }

    @Test
    void shouldPublishFilesFromPublishingRequestOnlyWhenPublishingRequestIsApproved()
        throws ApiGatewayException, IOException {
        var fileToPublish = randomUnpublishedFile();
        var file = randomUnpublishedFile();
        var publication = createPublicationWithFiles(file, fileToPublish);
        var completedPublishingRequest = persistCompletedPublishingRequestWithApprovedFiles(publication, fileToPublish);
        completedPublishingRequest.setFilesForApproval(Set.of(FileForApproval.fromFile(fileToPublish)));
        var event = createEvent(null, completedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var publishedFiles = getPublishedFiles(updatedPublication);

        assertThat(publishedFiles, hasSize(1));
        assertThat(publishedFiles.getFirst().getIdentifier(),
                   is(equalTo(fileToPublish.getIdentifier())));
    }

    private PublishingRequestCase persistCompletedPublishingRequestWithApprovedFiles(Publication publication,
                                                                                     File file) throws ApiGatewayException {
        var publishingRequest =  (PublishingRequestCase) PublishingRequestCase.fromPublication(publication)
                                                             .withOwner(UserInstance.fromPublication(publication).getUsername())
                                     .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation());
        publishingRequest.setStatus(TicketStatus.COMPLETED);
        publishingRequest.setApprovedFiles(Set.of(file.getIdentifier()));
        publishingRequest.setWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY);
        return (PublishingRequestCase) publishingRequest.persistNewTicket(ticketService);
    }

    private Publication createPublicationWithFiles(File file, File fileInPublishingRequest) throws ApiGatewayException {
        var publication = randomPublication();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(List.of(file, fileInPublishingRequest)));
        var persistedPublication = Resource.fromPublication(publication)
            .persistNew(resourceService, UserInstance.fromPublication(publication));
        resourceService.publishPublication(UserInstance.fromPublication(publication), persistedPublication.getIdentifier());
        return persistedPublication;
    }

    private static List<PublishedFile> getPublishedFiles(Publication updatedPublication) {
        return updatedPublication.getAssociatedArtifacts().stream()
                   .filter(PublishedFile.class::isInstance)
                   .map(PublishedFile.class::cast)
                   .toList();
    }

    private static List<AssociatedArtifact> getAssociatedFiles(Publication updatedPublication) {
        return updatedPublication.getAssociatedArtifacts().stream().filter(File.class::isInstance).toList();
    }

    private DoiRequest createDoiRequestTicket(Publication publication) {
        return (DoiRequest) attempt(
            () -> DoiRequest.fromPublication(publication).persistNewTicket(ticketService)).orElseThrow();
    }

    private Publication createDraftPublicationWithDoi() throws BadRequestException {
        var publication = randomPublication().copy()
                              .withStatus(PublicationStatus.DRAFT)
                              .withDoi(randomDoi())
                              .build();
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }

    private InputStream createEvent(TicketEntry pendingPublishingRequest,
                                    TicketEntry approvedPublishingRequest) throws IOException {
        var sampleEvent = eventBody(pendingPublishingRequest, approvedPublishingRequest);
        var eventReference = storeEventToS3AndGenerateEventReference(sampleEvent);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventReference);
    }
    
    private EventReference storeEventToS3AndGenerateEventReference(String sampleEvent) throws IOException {
        var blobUri = s3Driver.insertEvent(UnixPath.of(randomString()), sampleEvent);
        return new EventReference(DataEntryUpdateEvent.PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC, blobUri);
    }
    
    private String eventBody(TicketEntry pendingPublishingRequest,
                             TicketEntry approvedPublishingRequest) {
        return new DataEntryUpdateEvent(randomString(), pendingPublishingRequest, approvedPublishingRequest)
                   .toJsonString();
    }
    
    private Publication createUnpublishablePublication() throws BadRequestException {
        var publication = randomPublication();
        publication.getEntityDescription().setMainTitle(null);
        return Resource.fromPublication(publication).persistNew(resourceService,
            UserInstance.fromPublication(publication));
    }
    
    private PublishingRequestCase modifyPublishingRequest(PublishingRequestCase pendingPublishingRequest) {
        pendingPublishingRequest.setModifiedDate(randomInstant());
        return pendingPublishingRequest;
    }
    
    private Publication createPublication() throws BadRequestException {
        var publication = randomPublication();
        return Resource.fromPublication(publication).persistNew(resourceService,
                                                                UserInstance.fromPublication(publication));
    }
    
    private PublishingRequestCase pendingPublishingRequest(Publication publication) {
        return (PublishingRequestCase) PublishingRequestCase.fromPublication(publication)
                   .withOwner(UserInstance.fromPublication(publication).getUsername());
    }

    private AcceptedPublishingRequestEventHandler handlerWithResourceServiceThrowingExceptionWhenUpdatingPublication()
        throws ApiGatewayException {
        resourceService = mock(ResourceService.class);
        var s3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(s3Client, randomString());
        when(resourceService.getPublication(any(), any())).thenReturn(randomPublication());
        when(resourceService.updatePublication(any())).thenThrow(RuntimeException.class);
        return new AcceptedPublishingRequestEventHandler(resourceService, ticketService, s3Client);
    }

    private AcceptedPublishingRequestEventHandler handlerWithResourceServiceThrowingExceptionWhenFetchingTicket()
        throws ApiGatewayException {
        ticketService = mock(TicketService.class);
        var s3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(s3Client, randomString());
        when(ticketService.fetchTicket(any())).thenThrow(RuntimeException.class);
        return new AcceptedPublishingRequestEventHandler(resourceService, ticketService, s3Client);
    }

    private TicketEntry persistPublishingRequestContainingExistingUnpublishedFiles(Publication publication)
        throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) PublishingRequestCase.createNewTicket(publication, PublishingRequestCase.class,
                                                                                              SortableIdentifier::next)
                                                            .withOwner(UserInstance.fromPublication(publication).getUsername())
                                                            .withOwnerAffiliation(publication.getResourceOwner().getOwnerAffiliation());
        publishingRequest.withFilesForApproval(convertUnpublishedFilesToFilesForApproval(publication));
        return publishingRequest.persistNewTicket(ticketService);
    }

    private Set<FileForApproval> convertUnpublishedFilesToFilesForApproval(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(UnpublishedFile.class::isInstance)
                   .map(UnpublishedFile.class::cast)
                   .map(FileForApproval::fromFile)
                   .collect(Collectors.toSet());
    }
}