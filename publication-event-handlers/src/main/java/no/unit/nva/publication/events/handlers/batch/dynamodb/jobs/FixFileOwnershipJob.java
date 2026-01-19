package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import nva.commons.core.paths.UriWrapper;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.service.impl.ServiceWithTransactions;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixFileOwnershipJob extends ServiceWithTransactions
    implements DynamodbResourceBatchJobExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FixFileOwnershipJob.class);
  private static final String JOB_TYPE = "FIX_FILE_OWNERSHIP";
  private static final String TABLE_NAME_ENV = "TABLE_NAME";
  private static final String SIKT_AFFILIATION_IDENTIFIER = "20754.";

  private final TicketService ticketService;
  private final AmazonDynamoDB dynamoDbClient;
  private final String tableName;

  @JacocoGenerated
  public FixFileOwnershipJob() {
    this(
        TicketService.defaultService(),
        AmazonDynamoDBClientBuilder.defaultClient(),
        new Environment().readEnv(TABLE_NAME_ENV));
  }

  public FixFileOwnershipJob(
      TicketService ticketService, AmazonDynamoDB dynamoDbClient, String tableName) {
    super(dynamoDbClient);
    this.ticketService = ticketService;
    this.dynamoDbClient = dynamoDbClient;
    this.tableName = tableName;
  }

  @Override
  public void executeBatch(List<BatchWorkItem> workItems) {
    var transactItems =
        workItems.stream()
            .map(this::fetchFileDaoFromDynamoDB)
            .flatMap(Optional::stream)
            .filter(this::hasSiktOwnerAffiliation)
            .map(this::updateFileOwnership)
            .flatMap(Optional::stream)
            .map(this::toTransactWriteItem)
            .toList();

    if (!transactItems.isEmpty()) {
      LOGGER.info("Updating {} file entries with corrected ownership", transactItems.size());
      sendTransactionWriteRequest(newTransactWriteItemsRequest(transactItems));
    }
  }

  @Override
  public String getJobType() {
    return JOB_TYPE;
  }

  private Optional<FileDaoWithVersion> fetchFileDaoFromDynamoDB(BatchWorkItem workItem) {
    var primaryKey = createPrimaryKey(workItem);
    var request = new GetItemRequest().withTableName(tableName).withKey(primaryKey);
    var result = dynamoDbClient.getItem(request);

    return Optional.ofNullable(result.getItem())
        .map(FileDao::fromDynamoFormat)
        .map(dao -> new FileDaoWithVersion(dao, dao.getVersion()));
  }

  private Map<String, AttributeValue> createPrimaryKey(BatchWorkItem workItem) {
    var key = new HashMap<String, AttributeValue>();
    key.put(
        PRIMARY_KEY_PARTITION_KEY_NAME, new AttributeValue(workItem.dynamoDbKey().partitionKey()));
    key.put(PRIMARY_KEY_SORT_KEY_NAME, new AttributeValue(workItem.dynamoDbKey().sortKey()));
    return key;
  }

  private boolean hasSiktOwnerAffiliation(FileDaoWithVersion fileDaoWithVersion) {
    return Optional.ofNullable(fileDaoWithVersion.fileDao().getFileEntry().getOwnerAffiliation())
        .map(UriWrapper::fromUri)
        .map(UriWrapper::getLastPathElement)
        .filter(orgId -> orgId.startsWith(SIKT_AFFILIATION_IDENTIFIER))
        .isPresent();
  }

  private Optional<FileDaoWithVersion> updateFileOwnership(FileDaoWithVersion fileDaoWithVersion) {
    var fileEntry = fileDaoWithVersion.fileDao().getFileEntry();
    var resourceIdentifier = fileEntry.getResourceIdentifier();
    var customerId = fileEntry.getCustomerId();

    return fetchPublishingRequestTicket(customerId, resourceIdentifier)
        .map(PublishingRequestCase::getOwnerAffiliation)
        .filter(Objects::nonNull)
        .filter(ticketAffiliation -> !ticketAffiliation.equals(fileEntry.getOwnerAffiliation()))
        .map(
            newAffiliation ->
                createUpdatedFileDao(
                    fileEntry, newAffiliation, fileDaoWithVersion.originalVersion()));
  }

  private Optional<PublishingRequestCase> fetchPublishingRequestTicket(
      URI customerId, SortableIdentifier resourceIdentifier) {
    try {
      var ticket =
          ticketService.fetchTicketByResourceIdentifier(
              customerId, resourceIdentifier, PublishingRequestCase.class);
      if (ticket.isEmpty()) {
        LOGGER.warn("No PublishingRequestCase found for resource {}", resourceIdentifier);
      }
      return ticket;
    } catch (Exception exception) {
      LOGGER.warn(
          "Failed to fetch PublishingRequestCase for resource {}: {}",
          resourceIdentifier,
          exception.getMessage());
      return Optional.empty();
    }
  }

  private FileDaoWithVersion createUpdatedFileDao(
      FileEntry original, URI newOwnerAffiliation, UUID originalVersion) {
    LOGGER.info(
        "Updating file {} ownership from {} to {} for resource {}",
        original.getIdentifier(),
        original.getOwnerAffiliation(),
        newOwnerAffiliation,
        original.getResourceIdentifier());
    original.setOwnerAffiliation(newOwnerAffiliation);
    return new FileDaoWithVersion(FileDao.fromFileEntry(original), originalVersion);
  }

  private TransactWriteItem toTransactWriteItem(FileDaoWithVersion fileDaoWithVersion) {
    return newPutTransactionItemWithLocking(
        fileDaoWithVersion.fileDao(), fileDaoWithVersion.originalVersion(), tableName);
  }

  private record FileDaoWithVersion(FileDao fileDao, UUID originalVersion) {}
}
