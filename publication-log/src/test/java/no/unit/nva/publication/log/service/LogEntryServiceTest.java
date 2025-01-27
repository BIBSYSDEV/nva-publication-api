package no.unit.nva.publication.log.service;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.model.business.logentry.LogTopic.FILE_APPROVED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.FILE_REJECTED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.FILE_UPLOADED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_CREATED;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.clients.GetCustomerResponse;
import no.unit.nva.clients.GetUserResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.events.bodies.DataEntryUpdateEvent;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogEntryServiceTest extends ResourcesLocalTest {

    private ResourceService resourceService;
    private TicketService ticketService;
    private IdentityServiceClient identityServiceClient;
    private LogEntryService logEntryService;

    @BeforeEach
    public void setUp() throws NotFoundException {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        ticketService = getTicketService();
        identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getUser(any())).thenReturn(randomUser());
        when(identityServiceClient.getCustomerByCristinId(any())).thenReturn(randomCustomer());
        logEntryService = new LogEntryService(resourceService, identityServiceClient);
    }

    @Test
    void shouldCreateLogEntryWhenResourceEventIsPresent() throws BadRequestException {
        var publication = createPublication();
        logEntryService.persistLogEntry(getDataEntryUpdateEvent(Resource.fromPublication(publication)));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(PUBLICATION_CREATED, logEntries.getFirst().topic());
    }

    @Test
    void shouldCreateLogEntryWithUserUsernameOnlyWhenFailingWhenFetchingUser()
        throws BadRequestException, NotFoundException {
        var publication = createPublication();
        when(identityServiceClient.getUser(any())).thenThrow(new NotFoundException("User not found"));

        logEntryService.persistLogEntry(getDataEntryUpdateEvent(Resource.fromPublication(publication)));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        var logUser = logEntries.getFirst().performedBy();
        assertNotNull(logUser.userName());
        assertNull(logUser.cristinId());
    }

    @Test
    void shouldNotCreateLogEntryWhenConsumedEventHasResourceWithNewImageWhereResourceEventIsNull()
        throws BadRequestException {
        var publication = createPublication();
        Resource.fromPublication(publication).clearResourceEvent(resourceService);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertTrue(logEntries.isEmpty());
    }

    @Test
    void shouldCreateLogEntryWithTopicFileUploadedWhenDataEntryEventIsFileEntryCreateEvent()
        throws BadRequestException {
        var publication = createPublication();
        var fileEntry = FileEntry.create(randomPendingInternalFile(), publication.getIdentifier(),
                                         UserInstance.fromPublication(publication));
        fileEntry.persist(resourceService);
        logEntryService.persistLogEntry(createDataEntryUpdateEvent(null,fileEntry));
        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(FILE_UPLOADED, logEntries.getFirst().topic());
    }

    @Test
    void shouldCreateLogEntryWithTopicFileApprovedWhenDataEntryEventIsFileApprovedEvent()
        throws ApiGatewayException {
        var publication = createPublication();
        var file = randomPendingOpenFile();
        var pendingFileEntry = FileEntry.create(file, publication.getIdentifier(),
                                                UserInstance.fromPublication(publication));

        var approvedFileEntry = FileEntry.create(file, publication.getIdentifier(),
                                                 UserInstance.fromPublication(publication));
        approvedFileEntry.persist(resourceService);
        approvedFileEntry.approve(resourceService);

        persistTicketWithApprovedFile(publication, file);

        var dataEntryUpdateEvent = createDataEntryUpdateEvent(pendingFileEntry, approvedFileEntry.fetch(resourceService).orElseThrow());
        logEntryService.persistLogEntry(dataEntryUpdateEvent);
        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(FILE_APPROVED, logEntries.getFirst().topic());
    }

    @Test
    void shouldCreateLogEntryWithTopicFileRejectedWhenDataEntryEventIsFileRejectedEvent()
        throws ApiGatewayException {
        var publication = createPublication();
        var file = randomPendingOpenFile();
        var pendingFileEntry = FileEntry.create(file, publication.getIdentifier(),
                                                UserInstance.fromPublication(publication));

        var rejectedFileEntry = FileEntry.create(file, publication.getIdentifier(),
                                                 UserInstance.fromPublication(publication));
        rejectedFileEntry.persist(resourceService);
        rejectedFileEntry.reject(resourceService);

        persistTicketWithRejectedFile(publication, file);

        var dataEntryUpdateEvent = createDataEntryUpdateEvent(pendingFileEntry, rejectedFileEntry.fetch(resourceService).orElseThrow());
        logEntryService.persistLogEntry(dataEntryUpdateEvent);
        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(FILE_REJECTED, logEntries.getFirst().topic());
    }

    @Test
    void shouldCreateLogEntryWithUserUsernameOnlyWhenFechingCustomerFails()
        throws ApiGatewayException {
        var publication = createPublication();
        var file = randomPendingOpenFile();
        var pendingFileEntry = FileEntry.create(file, publication.getIdentifier(),
                                                UserInstance.fromPublication(publication));

        var approvedFileEntry = FileEntry.create(file, publication.getIdentifier(),
                                                 UserInstance.fromPublication(publication));
        approvedFileEntry.persist(resourceService);
        approvedFileEntry.approve(resourceService);

        persistTicketWithApprovedFile(publication, file);

        when(identityServiceClient.getUser(any())).thenThrow(new NotFoundException("User not found"));

        logEntryService.persistLogEntry(createDataEntryUpdateEvent(pendingFileEntry, approvedFileEntry));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        var logUser = logEntries.getFirst().performedBy();
        assertNotNull(logUser.userName());
        assertNull(logUser.cristinId());
    }

    private void persistTicketWithApprovedFile(Publication publication, File file) throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) TicketEntry.createNewTicket(publication, PublishingRequestCase.class,
                                                                                    SortableIdentifier::next);
        publishingRequest
            .withFilesForApproval(Set.of(file))
            .withOwner(randomString())
            .withOwnerAffiliation(randomUri())
            .persistNewTicket(ticketService);

        publishingRequest.publishApprovedFile();
        publishingRequest.setFinalizedBy(new Username(randomString()));
        publishingRequest.persistUpdate(ticketService);
    }

    private void persistTicketWithRejectedFile(Publication publication, File file) throws ApiGatewayException {
        var publishingRequest = (PublishingRequestCase) TicketEntry.createNewTicket(publication, PublishingRequestCase.class,
                                                                                    SortableIdentifier::next);
        publishingRequest
            .withFilesForApproval(Set.of(file))
            .withOwner(randomString())
            .withOwnerAffiliation(randomUri())
            .persistNewTicket(ticketService);
        publishingRequest.setFinalizedBy(new Username(randomString()));
        publishingRequest.persistUpdate(ticketService);
    }

    private static DataEntryUpdateEvent createDataEntryUpdateEvent(Entity oldImage, Entity newImage) {
        return new DataEntryUpdateEvent(null, oldImage, newImage);
    }

    private static DataEntryUpdateEvent getDataEntryUpdateEvent(Entity entity) {
        return new DataEntryUpdateEvent(null, entity, entity);
    }

    private Publication createPublication() throws BadRequestException {
        var publication = randomPublication();
        return resourceService.createPublication(UserInstance.fromPublication(publication), publication);
    }

    private GetUserResponse randomUser() {
        return GetUserResponse.builder()
                   .withInstitutionCristinId(randomUri())
                   .withFamilyName(randomString())
                   .withGivenName(randomString())
                   .withCristinId(randomUri())
                   .build();
    }

    private GetCustomerResponse randomCustomer() {
        return new GetCustomerResponse(randomUri(), UUID.randomUUID(), randomString(), randomString(), randomString(),
                                       randomUri());
    }
}