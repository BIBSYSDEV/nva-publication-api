package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.clients.cristin.CristinClient;
import no.unit.nva.clients.cristin.CristinPersonDto;
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
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.utils.CustomerList;
import no.unit.nva.publication.model.utils.CustomerSummary;
import no.unit.nva.publication.service.FakeCristinUnitsUtil;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.apigateway.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateVerificationStatusJobTest extends ResourcesLocalTest {

    private static final URI CRISTIN_ORG_ID =
        URI.create("https://api.dev.nva.aws.unit.no/cristin/organization/20754.0.0.0");
    private static final String CRISTIN_PERSON_PATH = "/cristin/person/";

    private ResourceService resourceService;
    private CristinClient cristinClient;
    private UpdateVerificationStatusJob updateVerificationStatusJob;

    @BeforeEach
    void setUp() {
        super.init();
        when(customerService.fetchCustomers())
            .thenReturn(new CustomerList(List.of(new CustomerSummary(randomUri(), CRISTIN_ORG_ID))));
        this.resourceService = new ResourceService(client, RESOURCES_TABLE_NAME, Clock.systemDefaultZone(),
                                                   uriRetriever, channelClaimClient, customerService,
                                                   new FakeCristinUnitsUtil());
        this.cristinClient = mock(CristinClient.class);
        this.updateVerificationStatusJob = new UpdateVerificationStatusJob(resourceService, cristinClient);
    }

    @Test
    void shouldUpdateVerificationStatusToVerifiedWhenCristinPersonIsVerified() throws NotFoundException {
        var cristinPersonId = createCristinPersonUri("12345");
        var publication = createPublicationWithContributor(cristinPersonId, ContributorVerificationStatus.NOT_VERIFIED);
        persistPublication(publication);

        mockCristinClientReturnsVerifiedPerson(cristinPersonId);

        var workItem = createWorkItemForPublication(publication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        var updatedContributor = updatedResource.getEntityDescription().getContributors().getFirst();

        assertEquals(ContributorVerificationStatus.VERIFIED,
                     updatedContributor.getIdentity().getVerificationStatus());
    }

    @Test
    void shouldUpdateVerificationStatusToNotVerifiedWhenCristinPersonIsNotVerified() throws NotFoundException {
        var cristinPersonId = createCristinPersonUri("12345");
        var publication = createPublicationWithContributor(cristinPersonId, ContributorVerificationStatus.VERIFIED);
        persistPublication(publication);

        mockCristinClientReturnsNotVerifiedPerson(cristinPersonId);

        var workItem = createWorkItemForPublication(publication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        var updatedContributor = updatedResource.getEntityDescription().getContributors().getFirst();

        assertEquals(ContributorVerificationStatus.NOT_VERIFIED,
                     updatedContributor.getIdentity().getVerificationStatus());
    }

    @Test
    void shouldSetCannotBeEstablishedWhenCristinClientReturnsEmpty() throws NotFoundException {
        var cristinPersonId = createCristinPersonUri("12345");
        var publication = createPublicationWithContributor(cristinPersonId, ContributorVerificationStatus.VERIFIED);
        persistPublication(publication);

        when(cristinClient.getPerson(any(URI.class))).thenReturn(Optional.empty());

        var workItem = createWorkItemForPublication(publication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        var updatedContributor = updatedResource.getEntityDescription().getContributors().getFirst();

        assertEquals(ContributorVerificationStatus.CANNOT_BE_ESTABLISHED,
                     updatedContributor.getIdentity().getVerificationStatus());
    }

    @Test
    void shouldNotUpdateWhenContributorHasNoCristinId() throws NotFoundException {
        var publication = createPublicationWithContributorWithoutCristinId();
        persistPublication(publication);

        var workItem = createWorkItemForPublication(publication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        var updatedContributor = updatedResource.getEntityDescription().getContributors().getFirst();

        assertEquals(null, updatedContributor.getIdentity().getVerificationStatus());
    }

    @Test
    void shouldNotUpdateWhenVerificationStatusIsUnchanged() throws NotFoundException {
        var cristinPersonId = createCristinPersonUri("12345");
        var publication = createPublicationWithContributor(cristinPersonId, ContributorVerificationStatus.VERIFIED);
        persistPublication(publication);

        mockCristinClientReturnsVerifiedPerson(cristinPersonId);

        var workItem = createWorkItemForPublication(publication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        var updatedContributor = updatedResource.getEntityDescription().getContributors().getFirst();

        assertEquals(ContributorVerificationStatus.VERIFIED,
                     updatedContributor.getIdentity().getVerificationStatus());
    }

    @Test
    void shouldHandleEmptyBatch() {
        updateVerificationStatusJob.executeBatch(List.of());
        assertNotNull(updateVerificationStatusJob);
    }

    @Test
    void shouldReturnCorrectJobType() {
        assertEquals("UPDATE_VERIFICATION_STATUS", updateVerificationStatusJob.getJobType());
    }

    @Test
    void shouldHandleMultipleContributors() throws NotFoundException {
        var cristinPersonId1 = createCristinPersonUri("11111");
        var cristinPersonId2 = createCristinPersonUri("22222");

        var contributor1 = createContributor(cristinPersonId1, ContributorVerificationStatus.NOT_VERIFIED, 1);
        var contributor2 = createContributor(cristinPersonId2, ContributorVerificationStatus.NOT_VERIFIED, 2);

        var publication = randomPublication(Textbook.class);
        publication.getEntityDescription().setContributors(List.of(contributor1, contributor2));
        persistPublication(publication);

        mockCristinClientReturnsVerifiedPerson(cristinPersonId1);
        mockCristinClientReturnsNotVerifiedPerson(cristinPersonId2);

        var workItem = createWorkItemForPublication(publication);
        updateVerificationStatusJob.executeBatch(List.of(workItem));

        var updatedResource = resourceService.getResourceByIdentifier(publication.getIdentifier());
        var updatedContributors = updatedResource.getEntityDescription().getContributors();

        assertEquals(ContributorVerificationStatus.VERIFIED,
                     updatedContributors.get(0).getIdentity().getVerificationStatus());
        assertEquals(ContributorVerificationStatus.NOT_VERIFIED,
                     updatedContributors.get(1).getIdentity().getVerificationStatus());
    }

    private URI createCristinPersonUri(String personId) {
        return URI.create("https://api.dev.nva.aws.unit.no" + CRISTIN_PERSON_PATH + personId);
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

    private void persistPublication(Publication publication) {
        var resource = Resource.fromPublication(publication);
        var dao = resource.toDao();
        client.putItem(new PutItemRequest().withTableName(RESOURCES_TABLE_NAME).withItem(dao.toDynamoFormat()));
    }

    private BatchWorkItem createWorkItemForPublication(Publication publication) {
        var resource = Resource.fromPublication(publication);
        var dao = (ResourceDao) resource.toDao();
        var key = new DynamodbResourceBatchDynamoDbKey(dao.getPrimaryKeyPartitionKey(), dao.getPrimaryKeySortKey());
        return new BatchWorkItem(key, "UPDATE_VERIFICATION_STATUS");
    }
}
