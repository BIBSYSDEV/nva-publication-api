package no.unit.nva.publication.log.service;

import static no.unit.nva.model.testing.PublicationGenerator.randomDoi;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomResourceOwner;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.publication.model.business.ThirdPartySystem.OTHER;
import static no.unit.nva.publication.model.business.logentry.LogTopic.FILE_UPLOADED;
import static no.unit.nva.publication.model.business.logentry.LogTopic.PUBLICATION_PUBLISHED;
import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import no.unit.nva.clients.CustomerDto;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.clients.UserDto;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.journal.AcademicArticle;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.FileLogEntry;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.logentry.LogUser;
import no.unit.nva.publication.model.business.publicationstate.CreatedResourceEvent;
import no.unit.nva.publication.model.business.publicationstate.DoiRequestedEvent;
import no.unit.nva.publication.model.business.publicationstate.ImportedResourceEvent;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.testutils.RandomDataGenerator;
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
        ticketService = getTicketService();
        resourceService = getResourceService(client);
        identityServiceClient = mock(IdentityServiceClient.class);
        var cristinClient = mock(CristinClient.class);
        when(identityServiceClient.getUser(any())).thenReturn(randomUser());
        when(identityServiceClient.getCustomerByCristinId(any())).thenReturn(randomCustomer());
        logEntryService = new LogEntryService(resourceService, identityServiceClient, cristinClient);
    }

    @Test
    void shouldCreateLogEntryWhenResourceEventIsPresent() throws BadRequestException {
        var publication = createPublishedPublication();
        logEntryService.persistLogEntry(Resource.fromPublication(publication));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(PUBLICATION_PUBLISHED, logEntries.getFirst().topic());
    }

    @Test
    void shouldNotCreateTheSameLogEntryMultipleTimesForResource() throws BadRequestException {
        var publication = createPublishedPublication();
        var resource = Resource.fromPublication(publication);
        var userInstance = UserInstance.fromPublication(publication);
        var resourceEvent = CreatedResourceEvent.create(userInstance, Instant.now());

        resource.setResourceEvent(resourceEvent);
        resourceService.updateResource(resource, userInstance);

        logEntryService.persistLogEntry(resource);

        resource.setResourceEvent(resourceEvent);
        resourceService.updateResource(resource, userInstance);

        logEntryService.persistLogEntry(resource);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(1, logEntries.size());
    }

    @Test
    void shouldCreateLogEntryWithUserUsernameOnlyWhenFailingWhenFetchingUser()
        throws BadRequestException, NotFoundException {
        var publication = createPublishedPublication();
        when(identityServiceClient.getUser(any())).thenThrow(new NotFoundException("User not found"));

        logEntryService.persistLogEntry(Resource.fromPublication(publication));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        var logUser = (LogUser) logEntries.getFirst().performedBy();
        assertNotNull(logUser.username());
        assertNull(logUser.id());
    }

    @Test
    void shouldCreateLogEntryWhenFileEventIsPresent() throws BadRequestException {
        var publication = createPublishedPublication();
        var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), UserInstance.fromPublication(publication));
        fileEntry.persist(resourceService, UserInstance.fromPublication(publication));
        fileEntry.approve(resourceService, new User(randomString()));

        logEntryService.persistLogEntry(fileEntry);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(FILE_UPLOADED, logEntries.getFirst().topic());
    }

    @Test
    void shouldCreateFileLogEntryWithUserUsernameOnlyWhenFailingWhenFetchingUser()
        throws BadRequestException, NotFoundException {
        when(identityServiceClient.getUser(any())).thenThrow(new NotFoundException("User not found"));

        var publication = createPublishedPublication();
        var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), UserInstance.fromPublication(publication));
        fileEntry.persist(resourceService, UserInstance.fromPublication(publication));
        fileEntry.approve(resourceService, new User(randomString()));

        logEntryService.persistLogEntry(fileEntry);
        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        var logUser = (LogUser) logEntries.getFirst().performedBy();
        assertNotNull(logUser.username());
    }

    @Test
    void shouldPersistLogEntryFromImportedResourceEvent() throws BadRequestException {
        var publication = createPublishedPublication();
        var resource = Resource.fromPublication(publication);
        var userInstance = UserInstance.fromPublication(publication);
        resource.setResourceEvent(ImportedResourceEvent.fromImportSource(
            ImportSource.fromBrageArchive("A"), userInstance,
            Instant.now()));
        resource.setDoi(randomDoi());
        resourceService.updateResource(resource, userInstance);
        logEntryService.persistLogEntry(Resource.fromPublication(publication));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(LogTopic.PUBLICATION_IMPORTED, logEntries.getFirst().topic());
    }

    @Test
    void shouldPersistLogEntryFromFileTypeUpdateByImportEvent() throws BadRequestException {
        var publication = createPublishedPublication();
        var userInstance = UserInstance.fromPublication(publication);
        var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), userInstance);
        fileEntry.persist(resourceService, userInstance);
        fileEntry.updateFromImport(randomOpenFile(), userInstance, ImportSource.fromBrageArchive(randomString()));
        fileEntry.toDao().updateExistingEntry(client);
        var updatedFileEntry = fileEntry.fetch(resourceService).orElseThrow();
        logEntryService.persistLogEntry(updatedFileEntry);
        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(LogTopic.FILE_TYPE_UPDATED_BY_IMPORT, logEntries.getFirst().topic());
    }

    @Test
    void shouldPersistFileUploadedLogEntryForThirdParty() throws BadRequestException {
        var publication = createPublishedPublication();
        var userInstance = UserInstance.createExternalUser(randomResourceOwner(), randomUri(), OTHER);
        var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), userInstance);
        fileEntry.persist(resourceService, userInstance);
        var updatedFileEntry = fileEntry.fetch(resourceService).orElseThrow();
        logEntryService.persistLogEntry(updatedFileEntry);
        var logEntry = (FileLogEntry) Resource.fromPublication(publication).fetchLogEntries(resourceService).getFirst();
        assertEquals(FILE_UPLOADED, logEntry.topic());
        assertNotNull(logEntry.importSource());
    }

    @Test
    void shouldPersistLogEntryWhenConsumingDoiRequest() throws ApiGatewayException {
        var publication = createPublishedPublication();
        var resource = Resource.fromPublication(publication);
        var doiRequest = createDoiRequestWithEvent(resource, publication);
        logEntryService.persistLogEntry(doiRequest);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(LogTopic.DOI_REQUESTED, logEntries.getFirst().topic());
    }

    private TicketEntry createDoiRequestWithEvent(Resource resource, Publication publication) throws ApiGatewayException {
        var doiRequest = DoiRequest.create(resource, UserInstance.fromPublication(publication));
        doiRequest.setTicketEvent(DoiRequestedEvent.create(UserInstance.fromPublication(publication), Instant.now()));
        return doiRequest.persistNewTicket(ticketService);
    }

    private Publication createPublishedPublication() throws BadRequestException {
        var publication = randomPublication(AcademicArticle.class);
        var userInstance = UserInstance.fromPublication(publication);
        var persistedPublication = resourceService.createPublication(userInstance, publication);
        return Resource.fromPublication(persistedPublication).publish(resourceService, userInstance).toPublication();
    }

    private UserDto randomUser() {
        return UserDto.builder()
                   .withInstitutionCristinId(randomUri())
                   .withFamilyName(randomString())
                   .withGivenName(randomString())
                   .withCristinId(randomUri())
                   .build();
    }


    private CustomerDto randomCustomer() {
        return new CustomerDto(RandomDataGenerator.randomUri(),
                               UUID.randomUUID(),
                               randomString(),
                               randomString(),
                               randomString(),
                               RandomDataGenerator.randomUri(),
                               randomString(),
                               randomBoolean(),
                               randomBoolean(),
                               randomBoolean(),
                               Collections.emptyList(),
                               new CustomerDto.RightsRetentionStrategy(randomString(),
                                                                       RandomDataGenerator.randomUri()),
                                randomBoolean());
    }
}