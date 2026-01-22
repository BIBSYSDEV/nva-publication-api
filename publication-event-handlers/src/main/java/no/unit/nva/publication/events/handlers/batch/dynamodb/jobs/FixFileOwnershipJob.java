package no.unit.nva.publication.events.handlers.batch.dynamodb.jobs;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.events.handlers.batch.dynamodb.BatchWorkItem;
import no.unit.nva.publication.events.handlers.batch.dynamodb.DynamodbResourceBatchJobExecutor;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.ServiceWithTransactions;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

public class FixFileOwnershipJob extends ServiceWithTransactions
    implements DynamodbResourceBatchJobExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FixFileOwnershipJob.class);
  private static final String JOB_TYPE = "FIX_FILE_OWNERSHIP";
  private static final String TABLE_NAME_ENV = "TABLE_NAME";

  private final ResourceService resourceService;
  private final DynamoDbClient dynamoDbClient;
  private final String tableName;

  @JacocoGenerated
  public FixFileOwnershipJob() {
    this(
        ResourceService.defaultService(),
        DynamoDbClient.create(),
        new Environment().readEnv(TABLE_NAME_ENV));
  }

  public FixFileOwnershipJob(
      ResourceService resourceService, DynamoDbClient dynamoDbClient, String tableName) {
    super(dynamoDbClient);
    this.resourceService = resourceService;
    this.dynamoDbClient = dynamoDbClient;
    this.tableName = tableName;
  }

  @Override
  public void executeBatch(List<BatchWorkItem> workItems) {
    var transactItems =
        workItems.stream()
            .map(this::fetchFileDaoFromDynamoDB)
            .flatMap(Optional::stream)
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
    var request = GetItemRequest.builder()
            .tableName(tableName)
            .key(workItem.dynamoDbKey().toPrimaryKey())
            .build();
    var result = dynamoDbClient.getItem(request);

    return Optional.ofNullable(result.item())
        .filter(item -> !item.isEmpty())
        .map(FileDao::fromDynamoFormat)
        .map(dao -> new FileDaoWithVersion(dao, dao.getVersion()));
  }

  private Optional<FileDaoWithVersion> updateFileOwnership(FileDaoWithVersion fileDaoWithVersion) {
    var fileEntry = fileDaoWithVersion.fileDao().getFileEntry();
    var resourceIdentifier = fileEntry.getResourceIdentifier();

    return fetchPublishingRequestTicket(resourceIdentifier)
        .map(PublishingRequestCase::getOwnerAffiliation)
        .filter(ticketAffiliation -> !ticketAffiliation.equals(fileEntry.getOwnerAffiliation()))
        .map(
            newAffiliation ->
                createUpdatedFileDao(
                    fileEntry, newAffiliation, fileDaoWithVersion.originalVersion()));
  }

  private Optional<PublishingRequestCase> fetchPublishingRequestTicket(
      SortableIdentifier resourceIdentifier) {
    try {
      var resource =
          Resource.resourceQueryObject(resourceIdentifier)
              .fetch(resourceService)
              .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceIdentifier));
      var ticket =
          resourceService
              .fetchAllTicketsForResource(resource)
              .filter(PublishingRequestCase.class::isInstance)
              .map(PublishingRequestCase.class::cast)
              .findFirst();
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
