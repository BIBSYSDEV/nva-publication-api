package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.instancetypes.book.Textbook;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchDynamoDbKey;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.FakeCristinUnitsUtil;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FixFileOwnershipJobTest extends ResourcesLocalTest {

  private static final String JOB_TYPE = "FIX_FILE_OWNERSHIP";
  private static final String SIKT_CRISTIN_ORG_ID = "20754.0.0.0";
  private static final URI SIKT_AFFILIATION =
      URI.create("https://api.nva.unit.no/cristin/organization/" + SIKT_CRISTIN_ORG_ID);
  private static final URI NTNU_AFFILIATION =
      URI.create("https://api.nva.unit.no/cristin/organization/194.0.0.0");

  private ResourceService resourceService;
  private TicketService ticketService;
  private AmazonDynamoDB dynamoDbClient;
  private FixFileOwnershipJob fixFileOwnershipJob;

  @BeforeEach
  void setUp() {
    super.init();
    this.resourceService =
        new ResourceService(
            client,
            RESOURCES_TABLE_NAME,
            Clock.systemDefaultZone(),
            uriRetriever,
            channelClaimClient,
            customerService,
            new FakeCristinUnitsUtil());
    this.ticketService = getTicketService();
    this.dynamoDbClient = spy(client);
    this.fixFileOwnershipJob =
        new FixFileOwnershipJob(ticketService, dynamoDbClient, RESOURCES_TABLE_NAME);
  }

  @Test
  void shouldUpdateFileOwnershipFromTicketWhenFileHasSiktAffiliationAndTicketHasDifferent()
      throws Exception {
    var publication = persistPublicationWithFile(SIKT_AFFILIATION);
    var fileEntry = getPersistedFileEntry(publication);

    persistPublishingRequestWithOwnerAffiliation(publication, NTNU_AFFILIATION);

    var workItem = createWorkItemForFile(fileEntry);
    fixFileOwnershipJob.executeBatch(List.of(workItem));

    var updatedFileEntry =
        FileEntry.queryObject(fileEntry.getIdentifier()).fetch(resourceService).orElseThrow();

    assertEquals(NTNU_AFFILIATION, updatedFileEntry.getOwnerAffiliation());
  }

  @Test
  void shouldNotUpdateFileWhenOwnerAffiliationIsNotSikt() throws Exception {
    var publication = persistPublicationWithFile(NTNU_AFFILIATION);
    var fileEntry = getPersistedFileEntry(publication);

    persistPublishingRequestWithOwnerAffiliation(publication, randomUri());

    var workItem = createWorkItemForFile(fileEntry);
    fixFileOwnershipJob.executeBatch(List.of(workItem));

    verify(dynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
  }

  @Test
  void shouldNotUpdateFileWhenNoTicketExists() throws Exception {
    var publication = persistPublicationWithFile(SIKT_AFFILIATION);
    var fileEntry = getPersistedFileEntry(publication);

    var workItem = createWorkItemForFile(fileEntry);
    fixFileOwnershipJob.executeBatch(List.of(workItem));

    verify(dynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
  }

  @Test
  void shouldNotUpdateWhenTicketOwnerAffiliationMatchesFileOwnerAffiliation() throws Exception {
    var publication = persistPublicationWithFile(SIKT_AFFILIATION);
    var fileEntry = getPersistedFileEntry(publication);

    persistPublishingRequestWithOwnerAffiliation(publication, SIKT_AFFILIATION);

    var workItem = createWorkItemForFile(fileEntry);
    fixFileOwnershipJob.executeBatch(List.of(workItem));

    verify(dynamoDbClient, never()).transactWriteItems(any(TransactWriteItemsRequest.class));
  }

  @Test
  void shouldHandleEmptyBatch() {
    assertDoesNotThrow(() -> fixFileOwnershipJob.executeBatch(List.of()));
  }

  @Test
  void shouldReturnCorrectJobType() {
    assertEquals(JOB_TYPE, fixFileOwnershipJob.getJobType());
  }

  private Publication persistPublicationWithFile(URI fileOwnerAffiliation) throws Exception {
    var publication = randomPublication(Textbook.class);
    var userInstance =
        createUserInstanceWithAffiliation(fileOwnerAffiliation, publication.getPublisher().getId());
    var persistedPublication =
        Resource.fromPublication(publication).persistNew(resourceService, userInstance);

    var file = randomPendingOpenFile();
    var fileEntry = FileEntry.create(file, persistedPublication.getIdentifier(), userInstance);
    fileEntry.persist(resourceService, userInstance);

    return persistedPublication;
  }

  private FileEntry getPersistedFileEntry(Publication publication) {
    return resourceService
        .fetchFileEntriesForResource(Resource.fromPublication(publication))
        .findFirst()
        .orElseThrow();
  }

  private void persistPublishingRequestWithOwnerAffiliation(
      Publication publication, URI ownerAffiliation) throws Exception {
    var ticket =
        TicketEntry.requestNewTicket(publication, PublishingRequestCase.class)
            .withOwnerAffiliation(ownerAffiliation)
            .withOwner(randomString());
    ticket.persistNewTicket(ticketService);
  }

  private UserInstance createUserInstanceWithAffiliation(URI ownerAffiliation, URI customerId) {
    return UserInstance.createBackendUser(
        new ResourceOwner(new Username(randomString()), ownerAffiliation), customerId);
  }

  private BatchWorkItem createWorkItemForFile(FileEntry fileEntry) {
    var fileDao = fileEntry.toDao();
    var key =
        new DynamodbResourceBatchDynamoDbKey(
            fileDao.getPrimaryKeyPartitionKey(), fileDao.getPrimaryKeySortKey());
    return new BatchWorkItem(key, JOB_TYPE);
  }
}
