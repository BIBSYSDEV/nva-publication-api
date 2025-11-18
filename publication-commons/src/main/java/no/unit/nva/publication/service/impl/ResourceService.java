package no.unit.nva.publication.service.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.publication.PublicationServiceConfig.defaultDynamoDbClient;
import static no.unit.nva.publication.model.business.Resource.resourceQueryObject;
import static no.unit.nva.publication.model.business.publicationchannel.PublicationChannelUtil.createPublicationChannelDao;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.model.utils.PublicationUtil.getAnthologyPublicationIdentifier;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.Lists;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.auth.uriretriever.RawContentRetriever;
import no.unit.nva.auth.uriretriever.UriRetriever;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportStatus;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.additionalidentifiers.CristinIdentifier;
import no.unit.nva.model.additionalidentifiers.ScopusIdentifier;
import no.unit.nva.publication.external.services.ChannelClaimClient;
import no.unit.nva.publication.model.DeletePublicationStatusResponse;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.ScanResultWrapper;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.ResourceRelationship;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.model.business.publicationstate.CreatedResourceEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.model.storage.IdentifierEntry;
import no.unit.nva.publication.model.storage.KeyField;
import no.unit.nva.publication.model.storage.LogEntryDao;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.ResourceRelationshipDao;
import no.unit.nva.publication.model.storage.importcandidate.DatabaseEntryWithData;
import no.unit.nva.publication.model.storage.importcandidate.ImportCandidateDao;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.publication.model.utils.CustomerService;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadMethodException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.GodClass", "PMD.AvoidDuplicateLiterals", "PMD.CouplingBetweenObjects"})
public class ResourceService extends ServiceWithTransactions {

    public static final int AWAIT_TIME_BEFORE_FETCH_RETRY = 50;
    public static final String RESOURCE_REFRESHED_MESSAGE = "Resource has been refreshed successfully: {}";
    public static final String RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE = "Resource cannot be deleted: ";
    public static final int MAX_SIZE_OF_BATCH_REQUEST = 5;
    public static final String NOT_PUBLISHABLE = "Publication is not publishable. Check main title and doi";
    public static final String ONLY_PUBLISHED_PUBLICATIONS_CAN_BE_UNPUBLISHED_ERROR_MESSAGE =
        "Only published " + "publications can be " + "unpublished";
    public static final String DELETE_PUBLICATION_ERROR_MESSAGE = "Only unpublished publication can be deleted";
    public static final String RESOURCE_TO_REFRESH_NOT_FOUND_MESSAGE = "Resource to refresh is not found: {}";
    private static final String IMPORT_CANDIDATE_HAS_BEEN_DELETED_MESSAGE = "Import candidate has been deleted: {}";
    private static final String SEPARATOR_ITEM = ",";
    private static final String SEPARATOR_TABLE = ";";
    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);
    private final String tableName;
    private final Clock clockForTimestamps;
    private final ReadResourceService readResourceService;
    private final UpdateResourceService updateResourceService;
    private final DeleteResourceService deleteResourceService;
    private final RawContentRetriever uriRetriever;
    private final ChannelClaimClient channelClaimClient;
    private final CounterService counterService;
    private final CustomerService customerService;
    private final CristinUnitsUtil cristinUnitsUtil;

    public ResourceService(AmazonDynamoDB dynamoDBClient,
                           String tableName, Clock clock,
                           RawContentRetriever uriRetriever,
                           ChannelClaimClient channelClaimClient,
                           CustomerService customerService,
                           CristinUnitsUtil cristinUnitsUtil) {
        super(dynamoDBClient);

        requireNonNull(dynamoDBClient, "DynamoDbClient is missing!");
        requireNonNull(tableName, "Table name is missing!");
        requireNonNull(uriRetriever, "UriRetriever is missing!");
        requireNonNull(channelClaimClient, "ChannelClaimClient is missing!");
        requireNonNull(customerService, "CustomerService is missing!");

        this.tableName = tableName;
        this.clockForTimestamps = clock;
        this.uriRetriever = uriRetriever;
        this.cristinUnitsUtil = cristinUnitsUtil;
        this.counterService = new CristinIdentifierCounterService(dynamoDBClient, this.tableName);
        this.channelClaimClient = channelClaimClient;
        this.readResourceService = new ReadResourceService(client, this.tableName);
        this.customerService = customerService;
        this.updateResourceService = new UpdateResourceService(client, this.tableName, clockForTimestamps,
                                                               readResourceService, uriRetriever, channelClaimClient,
                                                               customerService, cristinUnitsUtil);
        this.deleteResourceService = new DeleteResourceService(client, this.tableName, readResourceService);
    }

    @JacocoGenerated
    public static ResourceService defaultService() {
        return defaultService(RESOURCES_TABLE_NAME);
    }

    /**
     * Should not be used initiating resourceService for resource-table.
     *
     * @param tableName name of table
     * @return ResourceService
     */

    @JacocoGenerated
    public static ResourceService defaultService(String tableName) {
        var uriRetriever = new UriRetriever();
        return new ResourceService(defaultDynamoDbClient(), tableName, Clock.systemDefaultZone(), uriRetriever,
                                   ChannelClaimClient.create(uriRetriever), new CustomerService(uriRetriever),
                                   CristinUnitsUtil.defaultInstance());
    }

    public Publication createPublication(UserInstance userInstance, Publication inputData) throws BadRequestException {
        var currentTime = clockForTimestamps.instant();
        var newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(SortableIdentifier.next());
        newResource.setResourceOwner(createResourceOwner(userInstance));
        newResource.setPublisher(createOrganization(userInstance));
        newResource.setCreatedDate(currentTime);
        newResource.setModifiedDate(currentTime);
        setResourceEvent(userInstance, newResource, currentTime);
        setStatusOnNewPublication(userInstance, inputData, newResource, currentTime);
        return insertResource(newResource).toPublication();
    }

    private static void setResourceEvent(UserInstance userInstance, Resource newResource, Instant currentTime) {
        newResource.setResourceEvent(CreatedResourceEvent.create(userInstance, currentTime));
    }

    public Resource importResource(Resource resource, ImportSource importSource) {
        return insertImportedResource(resource, importSource);
    }

    public Publication createPublicationFromImportedEntry(Publication inputData, ImportSource importSource) {
        var now = clockForTimestamps.instant();
        if (nonNull(importSource)) {
            inputData.addImportDetail(new ImportDetail(now, importSource));
        }
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(SortableIdentifier.next());
        newResource.setPublishedDate(now);
        newResource.setCreatedDate(now);
        newResource.setModifiedDate(now);
        newResource.setStatus(PUBLISHED);
        return insertResource(newResource).toPublication();
    }

    public Publication updatePublicationByImportEntry(Publication publication, ImportSource importSource) {
        if (isNull(importSource)) {
            throw new IllegalArgumentException();
        }
        publication.addImportDetail(new ImportDetail(Instant.now(), importSource));
        return updateResourceService.updatePublicationButDoNotChangeStatus(publication);
    }

    /**
     * Persists importCandidate with updated database metadata fields.
     *
     * @param importCandidate importCandidate from external source
     * @return updated importCandidate that has been sent to persistence
     */
    public ImportCandidate persistImportCandidate(ImportCandidate importCandidate) {
        importCandidate.setModifiedDate(clockForTimestamps.instant());
        importCandidate.setCreatedDate(clockForTimestamps.instant());
        var dao = new ImportCandidateDao(importCandidate, SortableIdentifier.next());
        return insertResourceFromImportCandidate(dao);
    }

    public Publication markPublicationForDeletion(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return markResourceForDeletion(resourceQueryObject(userInstance, resourceIdentifier)).toPublication();
    }

    public void refreshFile(SortableIdentifier identifier) {
        FileEntry.queryObject(identifier).fetch(this)
            .ifPresent(fileEntry -> fileEntry.toDao().updateExistingEntry(client));
    }

    // TODO: Should we delete all tickets for delete draft publication?
    public void deleteDraftPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws BadRequestException {
        var daos = readResourceService.fetchResourceAndDoiRequestFromTheByResourceIndex(userInstance,
                                                                                        resourceIdentifier);
        var deleteFilesTransactions = deleteResourceFilesTransaction(resourceIdentifier);

        var transactionItems = transactionItemsForDraftPublicationDeletion(daos);
        transactionItems.addAll(deleteFilesTransactions);
        TransactWriteItemsRequest transactWriteItemsRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(transactWriteItemsRequest);
    }

    public void deleteAllResourceAssociatedEntries(URI customerId, SortableIdentifier resourceIdentifier) {
        var daoList = readResourceService.fetchAllResourceAssociatedEntries(customerId, resourceIdentifier).stream()
                          .map(dao -> new Delete().withTableName(tableName).withKey(dao.primaryKey()))
                          .map(delete -> new TransactWriteItem().withDelete(delete))
                          .toList();
        var transactionItems = new ArrayList<>(daoList);
        var sendTransactionRequest = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(sendTransactionRequest);
    }

    public DeletePublicationStatusResponse updatePublishedStatusToDeleted(SortableIdentifier resourceIdentifier) {
        return updateResourceService.updatePublishedStatusToDeleted(resourceIdentifier);
    }

    public ListingResult<Entity> scanResources(int pageSize, Map<String, AttributeValue> startMarker,
                                               Collection<KeyField> types) {
        var scanRequest = createScanRequestThatFiltersOutIdentityEntries(pageSize, startMarker, types);
        var scanResult = getClient().scan(scanRequest);
        var values = extractDatabaseEntries(scanResult.getItems());
        var isTruncated = thereAreMorePagesToScan(scanResult);
        return new ListingResult<>(values, scanResult.getLastEvaluatedKey(), isTruncated);
    }

    public ScanResultWrapper scanResourcesRaw(int pageSize, Map<String, AttributeValue> startMarker,
                                              Collection<KeyField> types) {
        return scanResourcesRaw(pageSize, startMarker, types, null, null);
    }

    public ScanResultWrapper scanResourcesRaw(int pageSize, Map<String, AttributeValue> startMarker,
                                              Collection<KeyField> types, Integer segment, Integer totalSegments) {
        var scanRequest = createScanRequestThatFiltersOutIdentityEntries(pageSize, startMarker, types, segment,
                                                                         totalSegments);
        var scanResult = getClient().scan(scanRequest);
        var isTruncated = thereAreMorePagesToScan(scanResult);
        return new ScanResultWrapper(scanResult.getItems(), scanResult.getLastEvaluatedKey(), isTruncated);
    }

    public void refreshResources(List<Entity> dataEntries) {
        final var refreshedEntries = refreshAndMigrate(dataEntries);
        var writeRequests = createWriteRequestsForBatchJob(refreshedEntries);
        writeToDynamoInBatches(writeRequests);
    }

    public void refreshResourcesByKeys(Collection<Map<String, AttributeValue>> keys) {
        var entities = getEntities(keys);
        refreshResources(entities);
    }

    private List<Entity> getEntities(Collection<Map<String, AttributeValue>> keys) {
        var batchGetItemRequest = new BatchGetItemRequest().withRequestItems(
            Map.of(tableName, new KeysAndAttributes().withKeys(keys)));
        var batchGetItemResult = client.batchGetItem(batchGetItemRequest);
        var items = batchGetItemResult.getResponses().get(tableName);
        return extractDatabaseEntries(items);
    }

    public Resource getResourceByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return readResourceService.getResourceByIdentifier(identifier)
                   .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE + identifier));
    }

    public List<Publication> getPublicationsByCristinIdentifier(String cristinIdentifier) {
        return readResourceService.getPublicationsByCristinIdentifier(cristinIdentifier)
                   .stream()
                   .map(Resource::fromPublication)
                   .map(Resource::getIdentifier)
                   .map(readResourceService::getResourceByIdentifier)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .map(Resource::toPublication)
                   .toList();
    }

    public List<Publication> getPublicationsByScopusIdentifier(ScopusIdentifier scopusIdentifier) {
        return readResourceService.getPublicationsByScopusIdentifier(scopusIdentifier.value())
                   .stream()
                   .map(Resource::fromPublication)
                   .map(Resource::getIdentifier)
                   .map(readResourceService::getResourceByIdentifier)
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .map(Resource::toPublication)
                   .toList();
    }

    public List<PublicationSummary> getPublicationSummaryByOwner(UserInstance sampleUser) {
        return readResourceService.getResourcesByOwner(sampleUser);
    }

    public Publication getPublicationByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return getResourceByIdentifier(identifier).toPublication();
    }

    public ImportCandidate getImportCandidateByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return readResourceService.getImportCandidateByIdentifier(identifier)
                   .orElseThrow(() -> new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE + identifier));
    }

    public ImportCandidate updateImportStatus(SortableIdentifier identifier, ImportStatus status)
        throws NotFoundException {
        return updateResourceService.updateStatus(identifier, status);
    }

    public void deleteImportCandidate(ImportCandidate importCandidate) throws BadMethodException {
        deleteResourceService.deleteImportCandidate(importCandidate);
        logger.info(IMPORT_CANDIDATE_HAS_BEEN_DELETED_MESSAGE, importCandidate.getIdentifier());
    }

    public Publication updatePublication(Publication resourceUpdate) {
        return updateResourceService.updatePublicationButDoNotChangeStatus(resourceUpdate);
    }

    public Resource updateResource(Resource resource, UserInstance userInstance) {
        return updateResourceService.updateResource(resource, userInstance);
    }

    // update this method according to current needs.
    public Entity migrate(Entity dataEntry) {
        try {
            return dataEntry;
        } catch (Exception e) {
            logger.error("Could not migrate data entry: {}, {}. Error: {}",
                         dataEntry.getType(),
                         dataEntry.getIdentifier(),
                         e.getMessage());
            throw new RuntimeException("Could not migrate data entry: " + dataEntry.getIdentifier(), e);
        }
    }

    public Stream<FileEntry> fetchFileEntriesForResource(Resource resource) {
        var partitionKeyValue = FileDao.getFileEntriesByResourceIdentifierPartitionKey(resource);

        var queryRequest = createQueryForFilesAssociatedWithResource(partitionKeyValue);
        return client.query(queryRequest)
                   .getItems()
                   .stream()
                   .map(FileDao::fromDynamoFormat)
                   .map(FileDao::getFileEntry);
    }

    public Resource updateResourceFromImport(Resource resource, UserInstance userInstance, ImportSource importSource) {
        return updateResourceService.updateResourceFromImport(resource, userInstance, importSource);
    }

    private QueryRequest createQueryForFilesAssociatedWithResource(String partitionKeyValue) {
        return new QueryRequest()
                   .withTableName(tableName)
                   .withIndexName(BY_TYPE_AND_IDENTIFIER_INDEX_NAME)
                   .withKeyConditionExpression("PK3 = :resourceIdentifier AND begins_with(SK3, :type)")
                   .withExpressionAttributeValues(
                       Map.of(
                           ":resourceIdentifier", new AttributeValue().withS(partitionKeyValue),
                           ":type", new AttributeValue().withS(FileDao.TYPE)
                       )
                   );
    }

    private void mutateResourceIfMissingCristinIdentifier(Resource resource) {
        if (resource.getCristinIdentifier().isEmpty()) {
            var counterEntry = counterService.next();
            injectSyntheticCristinIdentifier(resource, counterEntry.value());
        }
    }

    public Stream<TicketEntry> fetchAllTicketsForResource(Resource resource) {
        return readResourceService.fetchAllTicketsForResource(resource);
    }

    public void refreshResource(SortableIdentifier identifier) {
        try {
            var resource = getResourceByIdentifier(identifier);
            updateResourceService.refreshResource(resource);
            logger.info(RESOURCE_REFRESHED_MESSAGE, identifier);
        } catch (Exception e) {
            logger.error(RESOURCE_TO_REFRESH_NOT_FOUND_MESSAGE, identifier);
        }
    }

    public ImportCandidate updateImportCandidate(ImportCandidate importCandidate)
        throws BadRequestException, NotFoundException {
        return updateResourceService.updateImportCandidate(importCandidate);
    }

    public void unpublishPublication(Publication publication, UserInstance userInstance)
        throws BadRequestException, NotFoundException {
        var existingPublication = getResourceByIdentifier(publication.getIdentifier()).toPublication();
        if (!PUBLISHED.equals(existingPublication.getStatus())) {
            throw new BadRequestException(ONLY_PUBLISHED_PUBLICATIONS_CAN_BE_UNPUBLISHED_ERROR_MESSAGE);
        }
        var allTicketsForResource = fetchAllTicketsForResource(Resource.fromPublication(publication));
        updateResourceService.unpublishPublication(publication, allTicketsForResource,
                                                   userInstance);
    }

    public void terminateResource(Resource resource, UserInstance userInstance) throws BadRequestException {
        if (!UNPUBLISHED.equals(resource.getStatus())) {
            throw new BadRequestException(DELETE_PUBLICATION_ERROR_MESSAGE);
        }
        updateResourceService.terminateResource(resource, userInstance);
    }

    public void persistLogEntry(LogEntry logEntry) {
        var dao = LogEntryDao.fromLogEntry(logEntry);
        var put = new Put().withItem(dao.toDynamoFormat())
                      .withTableName(tableName)
                      .withConditionExpression(KEY_NOT_EXISTS_CONDITION)
                      .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES);
        var transactWriteItem = new TransactWriteItem().withPut(put);
        attempt(() -> getClient().transactWriteItems(newTransactWriteItemsRequest(transactWriteItem)));
    }

    public List<LogEntry> getLogEntriesForResource(Resource resource) {
        var partitionKeyValue = LogEntryDao.getLogEntriesByResourceIdentifierPartitionKey(resource);

        var queryRequest = new QueryRequest().withTableName(tableName)
                               .withKeyConditionExpression("PK0 = :value")
                               .withExpressionAttributeValues(
                                   Map.of(":value", new AttributeValue().withS(partitionKeyValue)));

        return client.query(queryRequest)
                   .getItems()
                   .stream()
                   .map(LogEntryDao::fromDynamoFormat)
                   .map(LogEntryDao::data)
                   .toList();
    }

    public void persistFile(FileEntry fileEntry) {
        var dao = fileEntry.toDao().createInsertionTransactionRequest();
        sendTransactionWriteRequest(dao);
    }

    public Optional<FileEntry> fetchFile(FileEntry fileEntry) {
        var primaryKey = fileEntry.toDao().primaryKey();
        var result = client.getItem(new GetItemRequest().withTableName(tableName).withKey(primaryKey));
        return Optional.ofNullable(result.getItem()).map(FileDao::fromDynamoFormat).map(FileEntry::fromDao);
    }

    public void deleteFile(FileEntry fileEntry) {
        var primaryKey = fileEntry.toDao().primaryKey();
        client.deleteItem(new DeleteItemRequest().withTableName(tableName).withKey(primaryKey));
    }

    public void updateFile(FileEntry fileEntry) {
        fileEntry.toDao().updateExistingEntry(client);
    }

    public ListingResult<PublicationChannel> fetchAllPublicationChannelsByIdentifier(SortableIdentifier identifier,
                                                                                     Map<String, AttributeValue> startMarker) {
        var queryRequest = new QueryRequest().withTableName(tableName)
                               .withKeyConditionExpression("PK0 = :value")
                               .withExclusiveStartKey(startMarker)
                               .withExpressionAttributeValues(
                                   Map.of(":value",
                                          new AttributeValue().withS("PublicationChannel:%s".formatted(identifier))));

        var result = client.query(queryRequest);
        var values = getPublicationChannels(result);
        var isTruncated = nonNull(result.getLastEvaluatedKey()) && !result.getLastEvaluatedKey().isEmpty();
        return new ListingResult<>(values, result.getLastEvaluatedKey(), isTruncated);
    }

    public void batchUpdateChannels(List<PublicationChannel> publicationChannels) {
        var writeRequests = publicationChannels.stream()
                                .map(PublicationChannel::toDao)
                                .map(PublicationChannelDao::toDynamoFormat)
                                .map(item -> new PutRequest().withItem(item))
                                .map(WriteRequest::new)
                                .toList();
        Lists.partition(writeRequests, 25)
            .stream()
            .map(items -> new BatchWriteItemRequest().withRequestItems(Map.of(tableName, items)))
            .forEach(this::writeBatchToDynamo);
    }

    private static List<PublicationChannel> getPublicationChannels(QueryResult result) {
        return result.getItems().stream().map(value -> parseAttributeValuesMap(value, Dao.class))
                   .filter(PublicationChannelDao.class::isInstance)
                   .map(PublicationChannelDao.class::cast)
                   .map(PublicationChannelDao::getData)
                   .map(PublicationChannel.class::cast)
                   .toList();
    }

    private Resource insertImportedResource(Resource resource, ImportSource importSource) {
        if (resource.getCuratingInstitutions().isEmpty()) {
            setCuratingInstitutions(resource, cristinUnitsUtil);
        }

        mutateResourceIfMissingCristinIdentifier(resource);

        var userInstance = UserInstance.fromPublication(resource.toPublication());
        var fileTransactionWriteItems = resource.getFiles()
                                            .stream()
                                            .map(
                                                file -> FileEntry.createFromImportSource(file, resource.getIdentifier(),
                                                                                         userInstance, importSource))
                                            .map(FileEntry::toDao)
                                            .map(dao -> dao.toPutNewTransactionItem(tableName))
                                            .toList();

        var transactions = new ArrayList<TransactWriteItem>();
        transactions.addAll(fileTransactionWriteItems);
        transactions.add(newPutTransactionItem(new ResourceDao(resource), tableName));
        transactions.add(createNewTransactionPutEntryForEnsuringUniqueIdentifier(resource));
        transactions.addAll(createPublicationChannelsTransaction(resource));

        var transactWriteItemsRequest = new TransactWriteItemsRequest().withTransactItems(transactions);
        sendTransactionWriteRequest(transactWriteItemsRequest);

        return resource;
    }

    private Collection<? extends TransactWriteItem> createPublicationChannelsTransaction(Resource resource) {
        var transactWriteItems = new ArrayList<TransactWriteItem>();

        resource.getPublisherWhenDegree().ifPresent(publisher -> {
            var publicationChannelDao = createPublicationChannelDao(channelClaimClient, resource, publisher);
            transactWriteItems.add(publicationChannelDao.toPutNewTransactionItem(tableName));
        });

        return transactWriteItems;
    }

    // TODO: Should we fetch files here?
    private List<TransactWriteItem> deleteResourceFilesTransaction(SortableIdentifier identifier) {
        var partitionKey = resourceQueryObject(identifier).toDao().getByTypeAndIdentifierPartitionKey();
        var queryRequest = new QueryRequest().withTableName(tableName)
                               .withIndexName(BY_TYPE_AND_IDENTIFIER_INDEX_NAME)
                               .withKeyConditionExpression("#PK3 = :value")
                               .withExpressionAttributeNames(Map.of("#PK3", "PK3"))
                               .withExpressionAttributeValues(Map.of(":value", new AttributeValue(partitionKey)));

        return client.query(queryRequest)
                   .getItems()
                   .stream()
                   .map(map -> parseAttributeValuesMap(map, Dao.class))
                   .filter(FileDao.class::isInstance)
                   .map(FileDao.class::cast)
                   .map(dao -> dao.toDeleteTransactionItem(tableName))
                   .toList();
    }

    @JacocoGenerated
    private void setStatusOnNewPublication(UserInstance userInstance, Publication fromPublication, Resource toResource,
                                           Instant currentTime)
        throws BadRequestException {
        var status = userInstance.isExternalClient() ? Optional.ofNullable(fromPublication.getStatus())
                                                           .orElse(PublicationStatus.DRAFT) : PublicationStatus.DRAFT;

        if (PUBLISHED.equals(status)) {
            if (!fromPublication.isPublishable()) {
                throw new BadRequestException(NOT_PUBLISHABLE);
            }
            toResource.setPublishedDate(currentTime);
        }

        toResource.setStatus(status);
    }

    private List<Entity> refreshAndMigrate(List<Entity> dataEntries) {
        return dataEntries.stream()
                   .map(attempt(this::migrate))
                   .map(Try::orElseThrow)
                   .toList();
    }

    private Organization createOrganization(UserInstance userInstance) {
        return new Organization.Builder().withId(userInstance.getCustomerId()).build();
    }

    private Owner createResourceOwner(UserInstance userInstance) {
        return new Owner(userInstance.getUsername(), userInstance.getTopLevelOrgCristinId());
    }

    private boolean thereAreMorePagesToScan(ScanResult scanResult) {
        return nonNull(scanResult.getLastEvaluatedKey()) && !scanResult.getLastEvaluatedKey().isEmpty();
    }

    private void writeToDynamoInBatches(List<WriteRequest> writeRequests) {
        Lists.partition(writeRequests, MAX_SIZE_OF_BATCH_REQUEST)
            .stream()
            .map(items -> new BatchWriteItemRequest().withRequestItems(Map.of(tableName, items)))
            .forEach(this::writeBatchToDynamo);
    }

    private void writeBatchToDynamo(BatchWriteItemRequest batchWriteItemRequest) {
        try {
            getClient().batchWriteItem(batchWriteItemRequest);
        } catch (Exception e) {
            var recordIdentifiers = extractRecordIdentifiers(batchWriteItemRequest);
            logger.warn("Failed to write batch to dynamo for the following resources: " + recordIdentifiers, e);
            // intentionally swallowing this exception to continue writing next batches
        }
    }

    private String extractRecordIdentifiers(BatchWriteItemRequest batchWriteItemRequest) {
        return batchWriteItemRequest.getRequestItems()
                   .values()
                   .stream()
                   .map(this::extractRecordIdentifiers)
                   .collect(Collectors.joining(SEPARATOR_TABLE));
    }

    private String extractRecordIdentifiers(List<WriteRequest> writeRequests) {
        return writeRequests.stream().map(this::extractPrimaryKeySortKey).collect(Collectors.joining(SEPARATOR_ITEM));
    }

    private String extractPrimaryKeySortKey(WriteRequest writeRequest) {
        return writeRequest.getPutRequest().getItem().get(PRIMARY_KEY_SORT_KEY_NAME).getS();
    }

    private List<WriteRequest> createWriteRequestsForBatchJob(List<Entity> refreshedEntries) {
        return refreshedEntries.stream()
                   .map(Entity::toDao)
                   .map(Dao::toDynamoFormat)
                   .map(item -> new PutRequest().withItem(item))
                   .map(WriteRequest::new)
                   .collect(Collectors.toList());
    }

    private ScanRequest createScanRequestThatFiltersOutIdentityEntries(int pageSize,
                                                                       Map<String, AttributeValue> startMarker,
                                                                       Collection<KeyField> types) {
        return createScanRequestThatFiltersOutIdentityEntries(pageSize, startMarker, types, null, null);
    }

    private ScanRequest createScanRequestThatFiltersOutIdentityEntries(int pageSize,
                                                                       Map<String, AttributeValue> startMarker,
                                                                       Collection<KeyField> types,
                                                                       Integer segment,
                                                                       Integer totalSegments) {
        var scanRequest = new ScanRequest().withTableName(tableName)
                              .withIndexName(DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME)
                              .withLimit(pageSize)
                              .withExclusiveStartKey(startMarker)
                              .withFilterExpression(Dao.scanFilterExpressionForDataEntries(types))
                              .withExpressionAttributeNames(Dao.scanFilterExpressionAttributeNames())
                              .withExpressionAttributeValues(Dao.scanFilterExpressionAttributeValues(types));

        if (nonNull(segment) && nonNull(totalSegments)) {
            scanRequest.withSegment(segment).withTotalSegments(totalSegments);
        }

        return scanRequest;
    }

    private List<Entity> extractDatabaseEntries(Collection<Map<String, AttributeValue>> items) {
        return items
                   .stream()
                   .filter(ResourceService::isNotLogEntry)
                   .map(value -> parseAttributeValuesMap(value, Dao.class))
                   .map(Dao::getData)
                   .toList();
    }

    private static boolean isNotLogEntry(Map<String, AttributeValue> map) {
        return !map.get("SK0").getS().contains("LogEntry");
    }

    private static void injectSyntheticCristinIdentifier(Resource resource,
                                                         int counterValue) {
        var additionalIdentifiers = new HashSet<>(resource.getAdditionalIdentifiers());
        additionalIdentifiers.add(CristinIdentifier.fromCounter(counterValue));
        resource.setAdditionalIdentifiers(additionalIdentifiers);
    }

    private Resource insertResource(Resource resource) {
        var transactions = new ArrayList<TransactWriteItem>();

        if (resource.getCuratingInstitutions().isEmpty()) {
            setCuratingInstitutions(resource, cristinUnitsUtil);
        }

        mutateResourceIfMissingCristinIdentifier(resource);

        var userInstance = UserInstance.fromPublication(resource.toPublication());
        var fileTransactionWriteItems = resource.getFiles()
                                            .stream()
                                            .map(file -> FileEntry.create(file, resource.getIdentifier(), userInstance))
                                            .map(FileEntry::toDao)
                                            .map(dao -> dao.toPutNewTransactionItem(tableName))
                                            .toList();

        transactions.addAll(fileTransactionWriteItems);
        transactions.add(newPutTransactionItem(new ResourceDao(resource), tableName));
        transactions.add(createNewTransactionPutEntryForEnsuringUniqueIdentifier(resource));
        transactions.addAll(createPublicationChannelsTransaction(resource));
        createResourceRelationshipTransaction(resource).ifPresent(transactions::add);

        var transactWriteItemsRequest = new TransactWriteItemsRequest().withTransactItems(transactions);
        sendTransactionWriteRequest(transactWriteItemsRequest);

        return resource;
    }

    private Optional<TransactWriteItem> createResourceRelationshipTransaction(Resource resource) {
        return getAnthologyPublicationIdentifier(resource)
            .map(identifier -> new ResourceRelationship(identifier, resource.getIdentifier()))
            .map(ResourceRelationshipDao::from)
            .map(DatabaseEntryWithData::toDynamoFormat)
            .map(this::putWithItem);
    }

    private TransactWriteItem putWithItem(Map<String, AttributeValue> item) {
        return new TransactWriteItem().withPut(new Put().withItem(item).withTableName(tableName));
    }

    private void setCuratingInstitutions(Resource newResource, CristinUnitsUtil cristinUnitsUtil) {
        newResource.setCuratingInstitutions(
            new CuratingInstitutionsUtil(uriRetriever, customerService).getCuratingInstitutions(
                newResource.toPublication().getEntityDescription(), cristinUnitsUtil));
    }

    private ImportCandidate insertResourceFromImportCandidate(ImportCandidateDao importCandidateDao) {
        client.putItem(tableName, importCandidateDao.toDynamoFormat());
        return importCandidateDao.getData();
    }

    private List<TransactWriteItem> transactionItemsForDraftPublicationDeletion(List<Dao> daos)
        throws BadRequestException {
        List<TransactWriteItem> transactionItems = new ArrayList<>();
        transactionItems.addAll(deleteResourceTransactionItems(daos));
        return transactionItems;
    }

    private List<TransactWriteItem> deleteResourceTransactionItems(List<Dao> daos) throws BadRequestException {
        ResourceDao resourceDao = extractResourceDao(daos);

        TransactWriteItem deleteResourceItem = newDeleteTransactionItem(resourceDao);
        applyDeleteResourceConditions(deleteResourceItem);

        TransactWriteItem deleteResourceIdentifierItem = newDeleteTransactionItem(IdentifierEntry.create(resourceDao));

        return List.of(deleteResourceItem, deleteResourceIdentifierItem);
    }

    private void applyDeleteResourceConditions(TransactWriteItem deleteResource) {
        Map<String, String> expressionAttributeNames = Map.of("#status", STATUS_FIELD_IN_RESOURCE);
        Map<String, AttributeValue> expressionAttributeValues = Map.of(":publishedStatus",
                                                                       new AttributeValue(DRAFT.getValue()));

        deleteResource.getDelete()
            .withConditionExpression("#status = :publishedStatus")
            .withExpressionAttributeNames(expressionAttributeNames)
            .withExpressionAttributeValues(expressionAttributeValues);
    }

    private Resource markResourceForDeletion(Resource resource) throws ApiGatewayException {
        return attempt(
            () -> updateResourceService.updatePublicationDraftToDraftForDeletion(resource.toPublication())).map(
            Resource::fromPublication).orElseThrow(failure -> markForDeletionError(failure, resource));
    }

    private ApiGatewayException markForDeletionError(Failure<Resource> failure, Resource resource) {
        if (primaryKeyConditionFailed(failure.getException())) {
            return new BadRequestException(RESOURCE_NOT_FOUND_MESSAGE + resource.getIdentifier().toString());
        } else if (failure.getException() instanceof IllegalStateException) {
            logger.warn(ExceptionUtils.stackTraceInSingleLine(failure.getException()));
            return new BadRequestException(
                RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE + resource.getIdentifier().toString());
        }
        throw new RuntimeException(failure.getException());
    }

    private boolean primaryKeyConditionFailed(Exception exception) {
        return exception instanceof NotFoundException && messageRefersToResourceNotFound(exception);
    }

    private boolean messageRefersToResourceNotFound(Exception exception) {
        return exception.getMessage().contains(RESOURCE_NOT_FOUND_MESSAGE);
    }

    private TransactWriteItem createNewTransactionPutEntryForEnsuringUniqueIdentifier(Resource resource) {
        return newPutTransactionItem(new IdentifierEntry(resource.getIdentifier().toString()), tableName);
    }
}
