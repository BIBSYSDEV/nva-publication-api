package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static java.util.UUID.randomUUID;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.clients.cristin.CristinPersonDto;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Publication;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.service.FakeCristinUnitsUtil;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateVerificationStatusJobTest extends ResourcesLocalTest {

    private static final String CRISTIN_PERSON_PATH = "/cristin/person/";
    private static final String API = "https://api.unittest.nva.aws.unit.no";
    private static final String JOB_TYPE = "UPDATE_VERIFICATION_STATUS";

    private ResourceService resourceService;
    private CristinClient cristinClient;
    private UpdateVerificationStatusJob updateVerificationStatusJob;

    @BeforeEach
    void setUp() {
        super.init();
        this.resourceService = spy(new ResourceService(client, RESOURCES_TABLE_NAME, Clock.systemDefaultZone(),
                                                       uriRetriever, channelClaimClient, customerService,
                                                       new FakeCristinUnitsUtil()));
        this.cristinClient = mock(CristinClient.class);
        this.updateVerificationStatusJob = new UpdateVerificationStatusJob(resourceService, cristinClient);
    }

    @Test
    void shouldUpdateVerificationStatusToVerifiedWhenCristinPersonIsVerified() throws Exception {
        var cristinPersonId = createCristinPersonUri("12345");
        var publication = createPublicationWithContributor(cristinPersonId, ContributorVerificationStatus.NOT_VERIFIED);
        var persistedPublication = persistPublication(publication);

        mockCristinClientReturnsVerifiedPerson(cristinPersonId);

        var workItem = createWorkItemForPublication(persistedPublication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        var updatedContributor = updatedResource.getEntityDescription().getContributors().getFirst();

        assertEquals(ContributorVerificationStatus.VERIFIED,
                     updatedContributor.getIdentity().getVerificationStatus());
    }

    @Test
    void shouldUpdateVerificationStatusToNotVerifiedWhenCristinPersonIsNotVerified() throws Exception {
        var cristinPersonId = createCristinPersonUri("12345");
        var publication = createPublicationWithContributor(cristinPersonId, ContributorVerificationStatus.VERIFIED);
        var persistedPublication = persistPublication(publication);

        mockCristinClientReturnsNotVerifiedPerson(cristinPersonId);

        var workItem = createWorkItemForPublication(persistedPublication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        var updatedContributor = updatedResource.getEntityDescription().getContributors().getFirst();

        assertEquals(ContributorVerificationStatus.NOT_VERIFIED,
                     updatedContributor.getIdentity().getVerificationStatus());
    }

    @Test
    void shouldSetNotVerifiedWhenCristinClientReturnsEmpty() throws Exception {
        var cristinPersonId = createCristinPersonUri("12345");
        var publication = createPublicationWithContributor(cristinPersonId, ContributorVerificationStatus.VERIFIED);
        var persistedPublication = persistPublication(publication);

        mockCristinClientReturnsEmpty();

        var workItem = createWorkItemForPublication(persistedPublication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        var updatedContributor = updatedResource.getEntityDescription().getContributors().getFirst();

        assertEquals(ContributorVerificationStatus.NOT_VERIFIED,
                     updatedContributor.getIdentity().getVerificationStatus());
    }

    @Test
    void shouldNotUpdateWhenContributorHasNoCristinId() throws Exception {
        var publication = createPublicationWithContributorWithoutCristinId();
        var persistedPublication = persistPublication(publication);

        var workItem = createWorkItemForPublication(persistedPublication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        verify(resourceService, never()).updateResource(any(), any());
    }

    @Test
    void shouldNotUpdateWhenVerificationStatusIsUnchanged() throws Exception {
        var cristinPersonId = createCristinPersonUri("12345");
        var publication = createPublicationWithContributor(cristinPersonId, ContributorVerificationStatus.VERIFIED);
        var persistedPublication = persistPublication(publication);

        mockCristinClientReturnsVerifiedPerson(cristinPersonId);

        var workItem = createWorkItemForPublication(persistedPublication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        verify(resourceService, never()).updateResource(any(), any());
    }

    @Test
    void shouldHandleEmptyBatch() {
        assertDoesNotThrow(() -> updateVerificationStatusJob.executeBatch(List.of()));
    }

    @Test
    void shouldThrowExceptionWhenResourceNotFound() {
        var nonExistentKey = getRandomKey();
        var workItem = new BatchWorkItem(nonExistentKey, JOB_TYPE);

        assertThrows(RuntimeException.class,
                     () -> updateVerificationStatusJob.executeBatch(List.of(workItem)));
    }

    @Test
    void shouldReturnCorrectJobType() {
        assertEquals(JOB_TYPE, updateVerificationStatusJob.getJobType());
    }

    @Test
    void shouldNotUpdateWhenContributorListIsEmpty() throws Exception {
        var publication = randomPublication(Textbook.class);
        publication.getEntityDescription().setContributors(List.of());
        var persistedPublication = persistPublication(publication);

        var workItem = createWorkItemForPublication(persistedPublication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        verify(resourceService, never()).updateResource(any(), any());
    }

    @Test
    void shouldPreserveContributorOrderAfterUpdate() throws Exception {
        var publication = randomPublication(Textbook.class);
        var contributors = List.of(
            createContributor(createCristinPersonUri("11111"), ContributorVerificationStatus.NOT_VERIFIED, 1),
            createContributor(createCristinPersonUri("22222"), ContributorVerificationStatus.VERIFIED, 2),
            createContributor(createCristinPersonUri("33333"), ContributorVerificationStatus.NOT_VERIFIED, 3)
        );
        publication.getEntityDescription().setContributors(contributors);
        var persistedPublication = persistPublication(publication);

        contributors.stream()
            .map(contributor -> contributor.getIdentity().getId())
            .forEach(this::mockCristinClientReturnsVerifiedPerson);

        var workItem = createWorkItemForPublication(persistedPublication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(persistedPublication.getIdentifier());
        var updatedContributors = updatedResource.getEntityDescription().getContributors();

        for (int index = 0; index < contributors.size(); index++) {
            assertEquals(contributors.get(index).getIdentity().getId(),
                         updatedContributors.get(index).getIdentity().getId());
            assertEquals(index + 1, updatedContributors.get(index).getSequence());
        }
    }

    private URI createCristinPersonUri(String personId) {
        return URI.create(API + CRISTIN_PERSON_PATH + personId);
    }

    private Publication createPublicationWithContributor(URI cristinPersonId,
                                                         ContributorVerificationStatus status) {
        var publication = randomPublication(Textbook.class);
        var contributor = createContributor(cristinPersonId, status, 1);
        publication.getEntityDescription().setContributors(List.of(contributor));
        return publication;
    }

    private Publication createPublicationWithContributorWithoutCristinId() {
        var publication = randomPublication(Textbook.class);
        var identity = new Identity.Builder()
                           .withName("Test Author")
                           .build();
        var contributor = new Contributor.Builder()
                              .withIdentity(identity)
                              .withRole(new RoleType(Role.CREATOR))
                              .withSequence(1)
                              .build();
        publication.getEntityDescription().setContributors(List.of(contributor));
        return publication;
    }

    private Contributor createContributor(URI cristinPersonId, ContributorVerificationStatus status, int sequence) {
        var identity = new Identity.Builder()
                           .withId(cristinPersonId)
                           .withName("Test Author")
                           .withVerificationStatus(status)
                           .build();
        return new Contributor.Builder()
                   .withIdentity(identity)
                   .withRole(new RoleType(Role.CREATOR))
                   .withSequence(sequence)
                   .build();
    }

    private void mockCristinClientReturnsVerifiedPerson(URI cristinPersonId) {
        var cristinPerson = new CristinPersonDto(cristinPersonId, Set.of(), Set.of(), Set.of(), true);
        when(cristinClient.getPerson(cristinPersonId)).thenReturn(Optional.of(cristinPerson));
    }

    private void mockCristinClientReturnsNotVerifiedPerson(URI cristinPersonId) {
        var cristinPerson = new CristinPersonDto(cristinPersonId, Set.of(), Set.of(), Set.of(), false);
        when(cristinClient.getPerson(cristinPersonId)).thenReturn(Optional.of(cristinPerson));
    }

    private void mockCristinClientReturnsEmpty() {
        when(cristinClient.getPerson(any(URI.class))).thenReturn(Optional.empty());
    }

    private Publication persistPublication(Publication publication) throws Exception {
        var resource = Resource.fromPublication(publication);
        var userInstance = UserInstance.fromPublication(publication);
        return resource.persistNew(resourceService, userInstance);
    }

    private BatchWorkItem createWorkItemForPublication(Publication publication) {
        var resource = Resource.fromPublication(publication);
        var dao = (ResourceDao) resource.toDao();
        var key = new DynamodbResourceBatchDynamoDbKey(dao.getPrimaryKeyPartitionKey(), dao.getPrimaryKeySortKey());
        return new BatchWorkItem(key, JOB_TYPE);
    }

    private static DynamodbResourceBatchDynamoDbKey getRandomKey() {
        return new DynamodbResourceBatchDynamoDbKey(
            "Resource:%s:%s@20754.0.0.0".formatted(randomUUID(), randomInteger()),
            "Resource:%s".formatted(SortableIdentifier.next()));
    }
}
