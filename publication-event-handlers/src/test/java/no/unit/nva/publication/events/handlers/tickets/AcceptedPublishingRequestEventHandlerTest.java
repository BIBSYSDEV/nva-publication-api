package no.unit.nva.publication.events.handlers.tickets;

import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import no.unit.nva.events.models.EventReference;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.AssociatedArtifact;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.RejectedFile;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationstate.DoiRequestedEvent;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AcceptedPublishingRequestEventHandlerTest extends ResourcesLocalTest {

    private static final Context CONTEXT = new FakeContext();
    private static final UserInstance USER_INSTANCE = UserInstance.create(randomString(), randomUri());
    private ResourceService resourceService;
    private TicketService ticketService;
    private AcceptedPublishingRequestEventHandler handler;
    private ByteArrayOutputStream outputStream;
    private S3Driver s3Driver;

    @BeforeEach
    void setup() {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        this.ticketService = getTicketService();
        var s3Client = new FakeS3Client();
        s3Driver = new S3Driver(s3Client, randomString());
        handler =
            new AcceptedPublishingRequestEventHandler(resourceService, ticketService, s3Client);
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldDoNothingWhenEventHasNoEffectiveChanges() throws ApiGatewayException, IOException {
        var publication =
            TicketTestUtils.createPersistedPublication(
                PublicationStatus.PUBLISHED, resourceService);
        var publishingRequest =
            pendingPublishingRequest(publication).complete(publication, USER_INSTANCE);
        var event = createEvent(publishingRequest, publishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var notUpdatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(
            publication.getModifiedDate(),
            is(equalTo(notUpdatedPublication.getModifiedDate())));
        assertThat(notUpdatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldPublishFilesWhenPublicationIsAlreadyPublished()
        throws ApiGatewayException, IOException {
        var publication =
            TicketTestUtils.createPersistedPublication(
                PublicationStatus.PUBLISHED, resourceService);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var approvedPublishingRequest =
            pendingPublishingRequest
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldPublishPublicationWhenPublishingRequestIsApproved()
        throws ApiGatewayException, IOException {
        var publication =
            TicketTestUtils.createPersistedPublication(
                PublicationStatus.PUBLISHED, resourceService);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var approvedPublishingRequest =
            pendingPublishingRequest
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    private static Stream<Arguments> workflowAndFileCombinationsProvider() {
        return Stream.of(
            Arguments.of(
                REGISTRATOR_PUBLISHES_METADATA_ONLY,
                randomPendingOpenFile(),
                OpenFile.class),
            Arguments.of(
                REGISTRATOR_PUBLISHES_METADATA_ONLY,
                randomPendingInternalFile(),
                InternalFile.class),
            Arguments.of(
                REGISTRATOR_PUBLISHES_METADATA_AND_FILES,
                randomPendingOpenFile(),
                OpenFile.class),
            Arguments.of(
                REGISTRATOR_PUBLISHES_METADATA_AND_FILES,
                randomPendingInternalFile(),
                InternalFile.class),
            Arguments.of(
                REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES,
                randomPendingOpenFile(),
                OpenFile.class),
            Arguments.of(
                REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES,
                randomPendingInternalFile(),
                InternalFile.class));
    }

    @ParameterizedTest
    @MethodSource("workflowAndFileCombinationsProvider")
    void shouldPublishFilesWhenPublishingRequestIsApproved(
        PublishingWorkflow publishingWorkflow,
        File file,
        Class<? extends File> expectedFileType)
        throws ApiGatewayException, IOException {
        var publication =
            TicketTestUtils.createPersistedPublicationWithFile(
                PublicationStatus.PUBLISHED, file, resourceService);
        var pendingPublishingRequest =
            (PublishingRequestCase)
                persistPublishingRequestContainingExistingUnpublishedFiles(publication);
        pendingPublishingRequest.setWorkflow(publishingWorkflow);
        var approvedPublishingRequest =
            pendingPublishingRequest
                .approveFiles()
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(getAssociatedFiles(updatedPublication), everyItem(instanceOf(expectedFileType)));
    }

    @Test
    void shouldNotPublishInternalFileWhenPublishingRequestIsApproved()
        throws ApiGatewayException, IOException {
        var publication =
            TicketTestUtils.createPersistedPublicationWithInternalFile(
                resourceService);
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest =
            pendingPublishingRequest
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertThat(
            updatedPublication.getAssociatedArtifacts().getFirst(),
            is(instanceOf(InternalFile.class)));
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldCreateDoiRequestTicketWhenPublicationWithDraftDoiIsPublished()
        throws IOException, ApiGatewayException {
        var publication = createDraftPublicationWithDoi();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest =
            pendingPublishingRequest
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket =
            ticketService
                .fetchTicketByResourceIdentifier(
                    publication.getPublisher().getId(),
                    publication.getIdentifier(),
                    DoiRequest.class)
                .orElseThrow();

        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(ticket.getStatus(), is(equalTo(TicketStatus.PENDING)));
        assertThat(
            ticket.getOwnerAffiliation(),
            is(equalTo(pendingPublishingRequest.getOwnerAffiliation())));
    }

    @Test
    void shouldCreateDoiRequestForTheSameInstitutionAsConsumedPublishingRequestTicketWhenPublishingMetadataOnPendingPublishingRequest()
        throws IOException, ApiGatewayException {
        var publication = createDraftPublicationWithDoi();
        var publishingRequest = pendingPublishingRequest(publication);
        publishingRequest.setWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var pendingPublishingRequest = publishingRequest.persistNewTicket(ticketService);
        var event = createEvent(null, pendingPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var ticket =
            ticketService
                .fetchTicketByResourceIdentifier(
                    publication.getPublisher().getId(),
                    publication.getIdentifier(),
                    DoiRequest.class)
                .orElseThrow();

        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(ticket.getStatus(), is(equalTo(TicketStatus.PENDING)));
        assertThat(ticket.getResourceStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(
            ticket.getOwnerAffiliation(),
            is(equalTo(pendingPublishingRequest.getOwnerAffiliation())));
        assertThat(ticket.getResponsibilityArea(), is(equalTo(publishingRequest.getResponsibilityArea())));
    }

    @Test
    void shouldCreateDoiRequestTicketForPublishedResourceAndSetTicketEventWithTheUserFromPublishingRequest()
        throws IOException, ApiGatewayException {
        var publication = createDraftPublicationWithDoi();
        var publishingRequest = pendingPublishingRequest(publication);
        publishingRequest.setWorkflow(REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var pendingPublishingRequest = publishingRequest.persistNewTicket(ticketService);
        var event = createEvent(null, pendingPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var ticket =
            ticketService
                .fetchTicketByResourceIdentifier(
                    publication.getPublisher().getId(),
                    publication.getIdentifier(),
                    DoiRequest.class)
                .orElseThrow();

        assertInstanceOf(DoiRequestedEvent.class, ticket.getTicketEvent());
        assertEquals(pendingPublishingRequest.getOwner(), ticket.getTicketEvent().user());
    }

    @Test
    void shouldNotCreateNewDoiRequestTicketWhenTicketAlreadyExists()
        throws ApiGatewayException, IOException {
        var publication = createDraftPublicationWithDoi();
        Resource.fromPublication(publication).publish(resourceService, UserInstance.fromPublication(publication));
        final var existingTicket =
            createDoiRequestTicket(
                Resource.fromPublication(publication).fetch(resourceService).orElseThrow().toPublication());
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest =
            pendingPublishingRequest
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var actualTicket =
            ticketService
                .fetchTicketByResourceIdentifier(
                    publication.getPublisher().getId(),
                    publication.getIdentifier(),
                    DoiRequest.class)
                .orElseThrow();

        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
        assertThat(existingTicket.getIdentifier(), is(equalTo(actualTicket.getIdentifier())));
        assertThat(actualTicket.getResourceStatus(), is(equalTo(PublicationStatus.PUBLISHED)));
    }

    @Test
    void shouldNotPublishPublicationWhenPublishingRequestIsStillNotApproved()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        var stillPendingPublishingRequest = modifyPublishingRequest(pendingPublishingRequest);
        var event = createEvent(pendingPublishingRequest, stillPendingPublishingRequest);

        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());
        assertThat(updatedPublication.getStatus(), is(equalTo(PublicationStatus.DRAFT)));
    }

    @Test
    void shouldThrowRuntimeExceptionAndLogExceptionMessageWhenPublishingPublicationFails()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishablePublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest =
            pendingPublishingRequest
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        var logger = LogUtils.getTestingAppenderForRootLogger();

        assertThrows(
            RuntimeException.class, () -> handler.handleRequest(event, outputStream, CONTEXT));

        assertThat(logger.getMessages(), containsString("Resource is not publishable"));
    }

    @Test
    void shouldThrowRuntimeExceptionAndLogExceptionMessageWhenUpdatingPublicationFails()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var pendingPublishingRequest =
            (PublishingRequestCase) persistPublishingRequestContainingExistingUnpublishedFiles(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest = pendingPublishingRequest.approveFiles()
                                            .complete(publication, USER_INSTANCE)
                                            .persistNewTicket(ticketService);
        var handlerThrowingException =
            handlerWithResourceServiceThrowingExceptionWhenUpdatingPublication();
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);

        assertThrows(
            RuntimeException.class,
            () -> handlerThrowingException.handleRequest(event, outputStream, CONTEXT));
    }

    @Test
    void shouldThrowRuntimeExceptionAndLogExceptionMessageWhenFetchingPublishingRequestFails()
        throws ApiGatewayException, IOException {
        var publication = createUnpublishablePublication();
        var pendingPublishingRequest = pendingPublishingRequest(publication);
        pendingPublishingRequest.setWorkflow(REGISTRATOR_REQUIRES_APPROVAL_FOR_METADATA_AND_FILES);
        var approvedPublishingRequest =
            pendingPublishingRequest
                .complete(publication, USER_INSTANCE)
                .persistNewTicket(ticketService);
        var handlerThrowingException =
            handlerWithResourceServiceThrowingExceptionWhenFetchingTicket();
        var event = createEvent(pendingPublishingRequest, approvedPublishingRequest);
        var logger = LogUtils.getTestingAppenderForRootLogger();

        assertThrows(
            RuntimeException.class,
            () -> handlerThrowingException.handleRequest(event, outputStream, CONTEXT));

        assertThat(logger.getMessages(), containsString("Could not fetch PublishingRequest"));
    }

    @Test
    void shouldPublishFilesFromPublishingRequestOnlyWhenPublishingRequestIsApproved()
        throws ApiGatewayException, IOException {
        var fileToPublish = randomPendingOpenFile();
        var file = randomPendingOpenFile();
        var publication = createPublicationWithFiles(file, fileToPublish);
        var completedPublishingRequest =
            persistCompletedPublishingRequestWithApprovedFiles(publication, fileToPublish);
        completedPublishingRequest.setFilesForApproval(Set.of(fileToPublish));
        var event = createEvent(null, completedPublishingRequest);
        handler.handleRequest(event, outputStream, CONTEXT);
        var updatedPublication =
            resourceService.getPublicationByIdentifier(publication.getIdentifier());
        var publishedFiles = getOpenFiles(updatedPublication);

        assertThat(publishedFiles, hasSize(1));
        assertThat(
            publishedFiles.getFirst().getIdentifier(),
            is(equalTo(fileToPublish.getIdentifier())));
    }

    @Test
    void shouldUpdateFilesFromPublishingRequestWhenOwnerAffiliationChanges()
        throws ApiGatewayException, IOException {
        var fileToPublish = randomPendingOpenFile();
        var file = randomPendingOpenFile();
        var publication = createPublicationWithFiles(file, fileToPublish);
        var publishingRequest = pendingPublishingRequest(publication, Set.of(fileToPublish));
        var newOwnerAffiliation = randomUri();
        var updatedPublishingRequest = publishingRequest.copy();

        updatedPublishingRequest.setReceivingOrganizationDetailsAndResetAssignee(newOwnerAffiliation,
                                                                                 newOwnerAffiliation);

        var event = createEvent(publishingRequest, updatedPublishingRequest);

        handler.handleRequest(event, outputStream, CONTEXT);

        var updatedPublication =
            resourceService.getResourceByIdentifier(publication.getIdentifier());

        assertThat(getActualOwnerAffiliation(updatedPublication, fileToPublish),
                   is(equalTo(newOwnerAffiliation)));
    }

    private static URI getActualOwnerAffiliation(Resource resource, File file) {
        return resource.getFileEntry(new SortableIdentifier(file.getIdentifier().toString()))
                   .orElseThrow()
                   .getOwnerAffiliation();
    }

    @Test
    void shouldNotUpdateFilesFromPublishingRequestWhenOwnerAffiliationUnchanged()
        throws ApiGatewayException, IOException {
        var fileToPublish = randomPendingOpenFile();
        var file = randomPendingOpenFile();
        var publication = createPublicationWithFiles(file, fileToPublish);
        var publishingRequest = pendingPublishingRequest(publication, Set.of(fileToPublish));
        var updatedPublishingRequest = publishingRequest.copy();

        updatedPublishingRequest.setReceivingOrganizationDetails(publishingRequest.getReceivingOrganizationDetails());

        var event = createEvent(publishingRequest, updatedPublishingRequest);

        handler.handleRequest(event, outputStream, CONTEXT);

        var updatedPublication =
            resourceService.getResourceByIdentifier(publication.getIdentifier());

        assertThat(getActualOwnerAffiliation(updatedPublication, fileToPublish),
                   is(not(equalTo(publishingRequest.getReceivingOrganizationDetails().topLevelOrganizationId()))));
    }

    @Test
    void shouldTriggerUpdateFilesFromPublishingRequestWhenNewTicket()
        throws ApiGatewayException, IOException {
        var file = randomPendingOpenFile();
        var publication = createPublicationWithFiles(file);
        var publishingRequest = pendingPublishingRequest(publication, Set.of(file)).persistNewTicket(ticketService);

        var event = createEvent(null, publishingRequest);

        handler.handleRequest(event, outputStream, CONTEXT);

        var updatedPublication =
            resourceService.getResourceByIdentifier(publication.getIdentifier());

        assertThat(getActualOwnerAffiliation(updatedPublication, file),
                   is(equalTo(publishingRequest.getReceivingOrganizationDetails().topLevelOrganizationId())));
    }

    @Test
    void shouldNotTriggerUpdateFilesFromPublishingRequestWhenDeletingTicket()
        throws ApiGatewayException, IOException {
        var file = randomPendingOpenFile();
        var publication = createPublicationWithFiles(file);
        var publishingRequest = pendingPublishingRequest(publication, Set.of(file));

        var event = createEvent(publishingRequest, null);

        handler.handleRequest(event, outputStream, CONTEXT);

        var updatedPublication =
            resourceService.getResourceByIdentifier(publication.getIdentifier());

        assertThat(getActualOwnerAffiliation(updatedPublication, file),
                   is(not(equalTo(publishingRequest.getReceivingOrganizationDetails().topLevelOrganizationId()))));
    }

    @Test
    void shouldProceedTicketOwnedByOtherInstitutionThanPublication() throws ApiGatewayException, IOException {
        var publication = createPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var publishingRequest = PublishingRequestCase.create(Resource.fromPublication(publication),
                                                             userInstance,
                                                             REGISTRATOR_PUBLISHES_METADATA_ONLY)
                                    .complete(publication, userInstance)
                                    .persistNewTicket(ticketService);
        var event = createEvent(null, publishingRequest);

        assertDoesNotThrow(() -> handler.handleRequest(event, outputStream, CONTEXT));
    }

    @Test
    void shouldHandleClosedPublishingRequestWhenPublishingRequestIsBeingRejected()
        throws ApiGatewayException, IOException {
        var publication = randomPublication();
        var internalFileToReject = randomPendingInternalFile();
        var openFileToReject = randomPendingOpenFile();
        publication.setAssociatedArtifacts(new AssociatedArtifactList(internalFileToReject, openFileToReject));
        publication = Resource.fromPublication(publication)
                          .persistNew(resourceService, UserInstance.fromPublication(publication));
        var publishingRequest = PublishingRequestCase
                                    .createWithFilesForApproval(Resource.fromPublication(publication),
                                                                UserInstance.fromPublication(publication),
                                                                REGISTRATOR_PUBLISHES_METADATA_ONLY,
                                                                Set.of(internalFileToReject, openFileToReject));
        var ticket = publishingRequest.persistNewTicket(ticketService);
        ticket.close(UserInstance.create(randomString(), randomUri())).persistUpdate(ticketService);

        var closedPublishingRequest = ticketService.fetchTicket(ticket);
        var event = createEvent(null, closedPublishingRequest);

        handler.handleRequest(event, outputStream, CONTEXT);

        var updatedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertTrue(updatedPublication.getAssociatedArtifacts().stream().allMatch(RejectedFile.class::isInstance));
    }

    @Test
    void shouldPublishPublicationWhenPendingPublishingRequestAndCustomerAllowsPublishingMetadata()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        publication.setAssociatedArtifacts(
            new AssociatedArtifactList(randomPendingInternalFile(), randomPendingOpenFile()));
        resourceService.updatePublication(publication);
        var publishingRequest = PublishingRequestCase.create(Resource.fromPublication(publication),
                                                             UserInstance.fromPublication(publication),
                                                             REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var ticket = publishingRequest.persistNewTicket(ticketService);
        var event = createEvent(null, ticket);

        handler.handleRequest(event, outputStream, CONTEXT);

        var publishedPublication = resourceService.getPublicationByIdentifier(publication.getIdentifier());

        assertEquals(PublicationStatus.PUBLISHED, publishedPublication.getStatus());
    }

    @Test
    void shouldRefreshPendingPublishingRequestWhenPublishingMetadataWithoutDoingAnyTicketUpdates()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        publication.setAssociatedArtifacts(
            new AssociatedArtifactList(randomPendingInternalFile(), randomPendingOpenFile()));
        resourceService.updatePublication(publication);
        var publishingRequest = PublishingRequestCase
                                    .create(Resource.fromPublication(publication),
                                            UserInstance.fromPublication(publication),
                                            REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var ticket = publishingRequest.persistNewTicket(ticketService);
        var event = createEvent(null, ticket);

        handler.handleRequest(event, outputStream, CONTEXT);

        var refreshedPublishingRequest = (PublishingRequestCase) publishingRequest.fetch(ticketService);

        assertNotEquals(publishingRequest, refreshedPublishingRequest);

        publishingRequest.setModifiedDate(null);
        refreshedPublishingRequest.setModifiedDate(null);

        assertEquals(publishingRequest, refreshedPublishingRequest);
    }

    @Test
    void shouldSetPublishingRequestOwnerAsThePersonWhoPublishedPublicationInResourceEventWhenPublishingPublicationByConsumingTicket()
        throws ApiGatewayException, IOException {
        var publication = createPublication();
        var publishingRequest = PublishingRequestCase
                                    .create(Resource.fromPublication(publication),
                                            UserInstance.fromPublication(publication),
                                            REGISTRATOR_PUBLISHES_METADATA_ONLY);
        var ticket = publishingRequest.persistNewTicket(ticketService);
        var event = createEvent(null, ticket);

        handler.handleRequest(event, outputStream, CONTEXT);

        var resource = Resource.resourceQueryObject(publication.getIdentifier())
                           .fetch(resourceService)
                           .orElseThrow();

        assertEquals(publishingRequest.getOwner().toString(), resource.getResourceEvent().user().toString());
    }

    @Test
    void shouldHandleFilesApprovalThesis() throws ApiGatewayException, IOException {
        var publication =
            TicketTestUtils.createPersistedDegreePublication(PublicationStatus.PUBLISHED, resourceService);
        var filesApprovalThesis = pendingFilesApprovalThesis(publication);
        var approvedTicket = filesApprovalThesis
                                 .complete(publication, USER_INSTANCE)
                                 .persistNewTicket(ticketService);
        var event = createEvent(filesApprovalThesis, approvedTicket);

        assertDoesNotThrow(() -> handler.handleRequest(event, outputStream, CONTEXT));
    }

    private PublishingRequestCase persistCompletedPublishingRequestWithApprovedFiles(
        Publication publication, File file) throws ApiGatewayException {
        var userInstance = UserInstance.fromPublication(publication);
        return (PublishingRequestCase) PublishingRequestCase.createWithFilesForApproval(
                Resource.fromPublication(publication),
                userInstance,
                REGISTRATOR_PUBLISHES_METADATA_ONLY,
                Set.of(file))
                                           .approveFiles().complete(publication, userInstance)
                                           .persistNewTicket(ticketService);
    }

    private Publication createPublicationWithFiles(File... files)
        throws ApiGatewayException {
        var publication = randomPublication();
        publication.setAssociatedArtifacts(
            new AssociatedArtifactList(List.of(files)));
        var persistedPublication =
            Resource.fromPublication(publication)
                .persistNew(resourceService, UserInstance.fromPublication(publication));
        Resource.fromPublication(persistedPublication)
            .publish(resourceService, UserInstance.fromPublication(publication));
        return persistedPublication;
    }

    private static List<OpenFile> getOpenFiles(Publication updatedPublication) {
        return updatedPublication.getAssociatedArtifacts().stream()
                   .filter(OpenFile.class::isInstance)
                   .map(OpenFile.class::cast)
                   .toList();
    }

    private static List<AssociatedArtifact> getAssociatedFiles(Publication updatedPublication) {
        return updatedPublication.getAssociatedArtifacts().stream()
                   .filter(File.class::isInstance)
                   .toList();
    }

    private DoiRequest createDoiRequestTicket(Publication publication) {
        return (DoiRequest)
                   attempt(
                       () ->
                           DoiRequest.create(Resource.fromPublication(publication),
                                             UserInstance.fromPublication(publication))
                               .persistNewTicket(ticketService))
                       .orElseThrow();
    }

    private Publication createDraftPublicationWithDoi() throws BadRequestException {
        var publication =
            randomPublication()
                .copy()
                .withStatus(PublicationStatus.DRAFT)
                .withDoi(randomDoi())
                .build();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private InputStream createEvent(
        TicketEntry pendingPublishingRequest, TicketEntry approvedPublishingRequest)
        throws IOException {
        var sampleEvent = eventBody(pendingPublishingRequest, approvedPublishingRequest);
        var eventReference = storeEventToS3AndGenerateEventReference(sampleEvent);
        return EventBridgeEventBuilder.sampleLambdaDestinationsEvent(eventReference);
    }

    private EventReference storeEventToS3AndGenerateEventReference(String sampleEvent)
        throws IOException {
        var blobUri = s3Driver.insertEvent(UnixPath.of(randomString()), sampleEvent);
        return new EventReference(
            DataEntryUpdateEvent.PUBLISHING_REQUEST_UPDATE_EVENT_TOPIC, blobUri);
    }

    private String eventBody(
        TicketEntry pendingPublishingRequest, TicketEntry approvedPublishingRequest) {
        return new DataEntryUpdateEvent(
            randomString(), pendingPublishingRequest, approvedPublishingRequest)
                   .toJsonString();
    }

    private Publication createUnpublishablePublication() throws BadRequestException {
        var publication = randomPublication();
        publication.getEntityDescription().setMainTitle(null);
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private PublishingRequestCase modifyPublishingRequest(
        PublishingRequestCase pendingPublishingRequest) {
        pendingPublishingRequest.setModifiedDate(randomInstant());
        return pendingPublishingRequest;
    }

    private Publication createPublication() throws BadRequestException {
        var publication = randomPublication();
        return Resource.fromPublication(publication)
                   .persistNew(resourceService, UserInstance.fromPublication(publication));
    }

    private PublishingRequestCase pendingPublishingRequest(Publication publication) {
        return pendingPublishingRequest(publication, Set.of());
    }

    private PublishingRequestCase pendingPublishingRequest(Publication publication, Set<File> files) {
        var userInstance = new UserInstance(randomString(), randomUri(), randomUri(), randomUri(), randomUri(),
                                            List.of(), UserClientType.INTERNAL);
        return PublishingRequestCase.createWithFilesForApproval(Resource.fromPublication(publication), userInstance,
                                                                REGISTRATOR_PUBLISHES_METADATA_ONLY, files);
    }

    private FilesApprovalThesis pendingFilesApprovalThesis(Publication publication) {
        var userInstance = new UserInstance(randomString(), randomUri(), randomUri(), randomUri(), randomUri(),
                                            List.of(), UserClientType.INTERNAL);
        return FilesApprovalThesis.createForUserInstitution(Resource.fromPublication(publication), userInstance,
                                                            REGISTRATOR_PUBLISHES_METADATA_ONLY);
    }

    private AcceptedPublishingRequestEventHandler
    handlerWithResourceServiceThrowingExceptionWhenUpdatingPublication()
        throws ApiGatewayException {
        resourceService = mock(ResourceService.class);
        var s3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(s3Client, randomString());
        when(resourceService.getPublicationByIdentifier(any())).thenReturn(randomPublication());
        when(resourceService.getResourceByIdentifier(any())).thenReturn(
            Resource.fromPublication(randomPublication().copy().withStatus(PublicationStatus.PUBLISHED).build()));
        when(resourceService.updateResource(any(), any())).thenReturn(Resource.fromPublication(randomPublication()));
        when(resourceService.updatePublication(any())).thenThrow(RuntimeException.class);
        return new AcceptedPublishingRequestEventHandler(resourceService, ticketService, s3Client);
    }

    private AcceptedPublishingRequestEventHandler
    handlerWithResourceServiceThrowingExceptionWhenFetchingTicket()
        throws ApiGatewayException {
        ticketService = mock(TicketService.class);
        var s3Client = new FakeS3Client();
        this.s3Driver = new S3Driver(s3Client, randomString());
        when(ticketService.fetchTicket(any())).thenThrow(RuntimeException.class);
        return new AcceptedPublishingRequestEventHandler(resourceService, ticketService, s3Client);
    }

    private TicketEntry persistPublishingRequestContainingExistingUnpublishedFiles(
        Publication publication) throws ApiGatewayException {
        var publishingRequest =
            (PublishingRequestCase)
                PublishingRequestCase.createNewTicket(
                        publication,
                        PublishingRequestCase.class,
                        SortableIdentifier::next)
                    .withOwner(UserInstance.fromPublication(publication).getUsername())
                    .withOwnerAffiliation(
                        publication.getResourceOwner().getOwnerAffiliation());
        publishingRequest.withFilesForApproval(
            TicketTestUtils.getFilesForApproval(publication));
        return publishingRequest.persistNewTicket(ticketService);
    }
}
