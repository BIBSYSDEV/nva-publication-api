package no.unit.nva.publication.log.service;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
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
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.clients.GetCustomerResponse;
import no.unit.nva.clients.GetUserResponse;
import no.unit.nva.clients.IdentityServiceClient;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogTopic;
import no.unit.nva.publication.model.business.publicationstate.ImportedResourceEvent;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogEntryServiceTest extends ResourcesLocalTest {

    private ResourceService resourceService;
    private IdentityServiceClient identityServiceClient;
    private LogEntryService logEntryService;

    @BeforeEach
    public void setUp() throws NotFoundException {
        super.init();
        resourceService = getResourceServiceBuilder().build();
        identityServiceClient = mock(IdentityServiceClient.class);
        when(identityServiceClient.getUser(any())).thenReturn(randomUser());
        when(identityServiceClient.getCustomerByCristinId(any())).thenReturn(randomCustomer());
        logEntryService = new LogEntryService(resourceService, identityServiceClient);
    }

    @Test
    void shouldCreateLogEntryWhenResourceEventIsPresent() throws BadRequestException {
        var publication = createPublication();
        logEntryService.persistLogEntry(Resource.fromPublication(publication));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(PUBLICATION_CREATED, logEntries.getFirst().topic());
    }

    @Test
    void shouldCreateLogEntryWithUserUsernameOnlyWhenFailingWhenFetchingUser()
        throws BadRequestException, NotFoundException {
        var publication = createPublication();
        when(identityServiceClient.getUser(any())).thenThrow(new NotFoundException("User not found"));

        logEntryService.persistLogEntry(Resource.fromPublication(publication));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        var logUser = logEntries.getFirst().performedBy();
        assertNotNull(logUser.username());
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
    void shouldCreateLogEntryWhenFileEventIsPresent() throws BadRequestException {
        var publication = createPublication();
        var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), UserInstance.fromPublication(publication));
        fileEntry.persist(resourceService);
        fileEntry.approve(resourceService, new User(randomString()));

        logEntryService.persistLogEntry(fileEntry);

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(FILE_UPLOADED, logEntries.getFirst().topic());
    }

    @Test
    void shouldCreateFileLogEntryWithUserUsernameOnlyWhenFailingWhenFetchingUser()
        throws BadRequestException, NotFoundException {
        when(identityServiceClient.getUser(any())).thenThrow(new NotFoundException("User not found"));

        var publication = createPublication();
        var fileEntry = FileEntry.create(randomOpenFile(), publication.getIdentifier(), UserInstance.fromPublication(publication));
        fileEntry.persist(resourceService);
        fileEntry.approve(resourceService, new User(randomString()));

        logEntryService.persistLogEntry(fileEntry);
        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        var logUser = logEntries.getFirst().performedBy();
        assertNotNull(logUser.username());
    }

    @Test
    void shouldPersistLogEntryFromImportedResourceEvent() throws BadRequestException {
        var publication = createPublication();
        var resource = Resource.fromPublication(publication);
        resource.setResourceEvent(ImportedResourceEvent.fromImportSource(ImportSource.fromBrageArchive("A"),
                                                                         Instant.now()));
        resourceService.updateResource(resource);
        logEntryService.persistLogEntry(Resource.fromPublication(publication));

        var logEntries = Resource.fromPublication(publication).fetchLogEntries(resourceService);

        assertEquals(LogTopic.PUBLICATION_IMPORTED, logEntries.getFirst().topic());
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