package no.unit.nva.publication.service.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.DRAFT;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.publication.model.business.Resource.resourceQueryObject;
import static no.unit.nva.publication.model.business.publicationchannel.PublicationChannelUtil.createPublicationChannelDao;
import static no.unit.nva.publication.model.business.publicationchannel.PublicationChannelUtil.toChannelClaimUri;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_NOT_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
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
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportDetail;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.external.services.ChannelClaimClient;
import no.unit.nva.publication.external.services.RawContentRetriever;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.model.DeletePublicationStatusResponse;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.model.business.logentry.LogEntry;
import no.unit.nva.publication.model.business.publicationchannel.ChannelType;
import no.unit.nva.publication.model.business.publicationchannel.NonClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.PublicationChannel;
import no.unit.nva.publication.model.business.publicationstate.CreatedResourceEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.model.storage.IdentifierEntry;
import no.unit.nva.publication.model.storage.KeyField;
import no.unit.nva.publication.model.storage.LogEntryDao;
import no.unit.nva.publication.model.storage.PublicationChannelDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.UniqueDoiRequestEntry;
import no.unit.nva.publication.model.storage.WithPrimaryKey;
import no.unit.nva.publication.model.utils.CuratingInstitutionsUtil;
import no.unit.nva.publication.storage.model.DatabaseConstants;
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

    public static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_SUPPLIER = SortableIdentifier::next;
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
    private final Supplier<SortableIdentifier> identifierSupplier;
    private final ReadResourceService readResourceService;
    private final UpdateResourceService updateResourceService;
    private final DeleteResourceService deleteResourceService;
    private final RawContentRetriever uriRetriever;
    private final ChannelClaimClient channelClaimClient;

    protected ResourceService(AmazonDynamoDB dynamoDBClient, String tableName, Clock clock,
                              Supplier<SortableIdentifier> identifierSupplier, RawContentRetriever uriRetriever,
                              ChannelClaimClient channelClaimClient) {
        super(dynamoDBClient);
        this.tableName = tableName;
        this.clockForTimestamps = clock;
        this.identifierSupplier = identifierSupplier;
        this.uriRetriever = uriRetriever;
        this.channelClaimClient = channelClaimClient;
        this.readResourceService = new ReadResourceService(client, this.tableName);
        this.updateResourceService = new UpdateResourceService(client, this.tableName, clockForTimestamps,
                                                               readResourceService, uriRetriever, channelClaimClient);
        this.deleteResourceService = new DeleteResourceService(client, this.tableName, readResourceService);
    }

    @JacocoGenerated
    public static ResourceService defaultService() {
        return builder().withChannelClaimClient(ChannelClaimClient.create(new UriRetriever())).build();
    }

    /**
     * Should not be used initiating resourceService for resource-table.
     *
     * @param tableName name of table
     * @return ResourceService
     */

    @JacocoGenerated
    public static ResourceService defaultService(String tableName) {
        return builder().withTableName(tableName).build();
    }

    public static ResourceServiceBuilder builder() {
        return new ResourceServiceBuilder();
    }

    public Publication createPublication(UserInstance userInstance, Publication inputData) throws BadRequestException {
        Instant currentTime = clockForTimestamps.instant();
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setResourceOwner(createResourceOwner(userInstance));
        newResource.setPublisher(createOrganization(userInstance));
        newResource.setCreatedDate(currentTime);
        newResource.setModifiedDate(currentTime);
        newResource.setResourceEvent(CreatedResourceEvent.create(userInstance, currentTime));
        setStatusOnNewPublication(userInstance, inputData, newResource);
        return insertResource(newResource).toPublication();
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
        newResource.setIdentifier(identifierSupplier.get());
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
        var now = clockForTimestamps.instant();
        var newResource = Resource.fromImportCandidate(importCandidate);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setPublishedDate(now);
        newResource.setCreatedDate(now);
        newResource.setModifiedDate(now);
        return insertResourceFromImportCandidate(newResource);
    }

    // @deprecated Only here for existing tests.
    @Deprecated(forRemoval = true)
    public Publication insertPreexistingPublication(Publication publication) {
        Resource resource = Resource.fromPublication(publication);
        return insertResource(resource).toPublication();
    }

    public Publication markPublicationForDeletion(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return markResourceForDeletion(resourceQueryObject(userInstance, resourceIdentifier)).toPublication();
    }

    public void refreshFile(SortableIdentifier identifier) {
        FileEntry.queryObject(identifier).fetch(this).orElseThrow().toDao().updateExistingEntry(client);
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

    public DeletePublicationStatusResponse updatePublishedStatusToDeleted(SortableIdentifier resourceIdentifier) {
        return updateResourceService.updatePublishedStatusToDeleted(resourceIdentifier);
    }

    public ListingResult<Entity> scanResources(int pageSize, Map<String, AttributeValue> startMarker,
                                               List<KeyField> types) {
        var scanRequest = createScanRequestThatFiltersOutIdentityEntries(pageSize, startMarker, types);
        var scanResult = getClient().scan(scanRequest);
        var values = extractDatabaseEntries(scanResult);
        var isTruncated = thereAreMorePagesToScan(scanResult);
        return new ListingResult<>(values, scanResult.getLastEvaluatedKey(), isTruncated);
    }

    public void refreshResources(List<Entity> dataEntries) {
        final var refreshedEntries = refreshAndMigrate(dataEntries);
        var writeRequests = createWriteRequestsForBatchJob(refreshedEntries);
        writeToDynamoInBatches(writeRequests);
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

    public List<PublicationSummary> getPublicationSummaryByOwner(UserInstance sampleUser) {
        return readResourceService.getResourcesByOwner(sampleUser);
    }

    public Publication getPublicationByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return getResourceByIdentifier(identifier).toPublication();
    }

    public ImportCandidate getImportCandidateByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return getResourceByIdentifier(identifier).toImportCandidate();
    }

    public ImportCandidate updateImportStatus(SortableIdentifier identifier, ImportStatus status)
        throws NotFoundException {
        return updateResourceService.updateStatus(identifier, status);
    }

    public void deleteImportCandidate(ImportCandidate importCandidate) throws BadMethodException, NotFoundException {
        deleteResourceService.deleteImportCandidate(importCandidate);
        logger.info(IMPORT_CANDIDATE_HAS_BEEN_DELETED_MESSAGE, importCandidate.getIdentifier());
    }

    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException {
        updateResourceService.updateOwner(identifier, oldOwner, newOwner);
    }

    public Publication updatePublication(Publication resourceUpdate) {
        return updateResourceService.updatePublicationButDoNotChangeStatus(resourceUpdate);
    }

    public Resource updateResource(Resource resource, UserInstance userInstance) {
        return updateResourceService.updateResource(resource, userInstance);
    }

    // update this method according to current needs.
    public Entity migrate(Entity dataEntry) {
        if (dataEntry instanceof Resource resource) {
            persistPublicationChannelForPublisherWhenDegree(resource);
        }
        return dataEntry;
    }

    private void persistPublicationChannelForPublisherWhenDegree(Resource resource) {
        resource.getPublisherWhenDegree().ifPresent(publisher -> {
            var channelClaimId = toChannelClaimUri(publisher.getIdentifier());
            var channelType = ChannelType.fromChannelId(publisher.getId());
            var nonClaimedPublicationChannelDao = NonClaimedPublicationChannel
                                                      .create(channelClaimId, resource.getIdentifier(), channelType)
                                                      .toDao();
            var transactionItem = toPutTransactionItem(nonClaimedPublicationChannelDao, tableName);
            sendTransactionWriteRequest(new TransactWriteItemsRequest().withTransactItems(transactionItem));
        });
    }

    public TransactWriteItem toPutTransactionItem(PublicationChannelDao dao, String tableName) {
        var put = new Put().withItem(dao.toDynamoFormat()).withTableName(tableName);
        return new TransactWriteItem().withPut(put);
    }

    public Stream<TicketEntry> fetchAllTicketsForResource(Resource resource) {
        return readResourceService.fetchAllTicketsForResource(resource);
    }

    public void refresh(SortableIdentifier identifier) {
        try {
            updatePublication(getPublicationByIdentifier(identifier));
            logger.info(RESOURCE_REFRESHED_MESSAGE, identifier);
        } catch (Exception e) {
            logger.error(RESOURCE_TO_REFRESH_NOT_FOUND_MESSAGE, identifier);
        }
    }

    public ImportCandidate updateImportCandidate(ImportCandidate importCandidate) throws BadRequestException {
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
                                   Map.of(":value", new AttributeValue().withS("PublicationChannel:%s".formatted(identifier))));

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
            setCuratingInstitutions(resource);
        }

        var userInstance = UserInstance.fromPublication(resource.toPublication());
        var fileTransactionWriteItems = resource.getFiles()
                                            .stream()
                                            .map(
                                                file -> FileEntry.createFromImportSource(file, resource.getIdentifier(),
                                                                                         userInstance, importSource))
                                            .map(FileEntry::toDao)
                                            .map(dao -> dao.toPutNewTransactionItem(tableName))
                                            .toList();

        var transactions = new ArrayList<>(fileTransactionWriteItems);
        transactions.add(newPutTransactionItem(new ResourceDao(resource), tableName));
        transactions.add(createNewTransactionPutEntryForEnsuringUniqueIdentifier(resource));

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
    private void setStatusOnNewPublication(UserInstance userInstance, Publication fromPublication, Resource toResource)
        throws BadRequestException {
        var status = userInstance.isExternalClient() ? Optional.ofNullable(fromPublication.getStatus())
                                                           .orElse(PublicationStatus.DRAFT) : PublicationStatus.DRAFT;

        if (status == PUBLISHED && !fromPublication.isPublishable()) {
            throw new BadRequestException(NOT_PUBLISHABLE);
        }

        toResource.setStatus(status);
    }

    private List<Entity> refreshAndMigrate(List<Entity> dataEntries) {
        return dataEntries.stream().map(attempt(this::migrate)).map(Try::orElseThrow).toList();
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
                                                                       List<KeyField> types) {
        return new ScanRequest().withTableName(tableName)
                   .withIndexName(DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME)
                   .withLimit(pageSize)
                   .withExclusiveStartKey(startMarker)
                   .withFilterExpression(Dao.scanFilterExpressionForDataEntries(types))
                   .withExpressionAttributeNames(Dao.scanFilterExpressionAttributeNames())
                   .withExpressionAttributeValues(Dao.scanFilterExpressionAttributeValues(types));
    }

    private List<Entity> extractDatabaseEntries(ScanResult response) {
        return response.getItems()
                   .stream()
                   .filter(ResourceService::isNotLogEntry)
                   .map(value -> parseAttributeValuesMap(value, Dao.class))
                   .map(Dao::getData)
                   .toList();
    }

    private static boolean isNotLogEntry(Map<String, AttributeValue> map) {
        return !map.get("SK0").getS().contains("LogEntry");
    }

    private Resource insertResource(Resource resource) {
        if (resource.getCuratingInstitutions().isEmpty()) {
            setCuratingInstitutions(resource);
        }

        var userInstance = UserInstance.fromPublication(resource.toPublication());
        var fileTransactionWriteItems = resource.getFiles()
                                            .stream()
                                            .map(file -> FileEntry.create(file, resource.getIdentifier(), userInstance))
                                            .map(FileEntry::toDao)
                                            .map(dao -> dao.toPutNewTransactionItem(tableName))
                                            .toList();

        var transactions = new ArrayList<>(fileTransactionWriteItems);
        transactions.add(newPutTransactionItem(new ResourceDao(resource), tableName));
        transactions.add(createNewTransactionPutEntryForEnsuringUniqueIdentifier(resource));
        transactions.addAll(createPublicationChannelsTransaction(resource));

        var transactWriteItemsRequest = new TransactWriteItemsRequest().withTransactItems(transactions);
        sendTransactionWriteRequest(transactWriteItemsRequest);

        return resource;
    }

    private void setCuratingInstitutions(Resource newResource) {
        newResource.setCuratingInstitutions(
            CuratingInstitutionsUtil.getCuratingInstitutionsOnline(newResource.toPublication(), uriRetriever));
    }

    private ImportCandidate insertResourceFromImportCandidate(Resource newResource) {
        TransactWriteItem[] transactionItems = transactionItemsForNewImportCandidateInsertion(newResource);

        var fileTransactionWriteItems = newResource.getFiles()
                                            .stream()
                                            .map(file -> FileEntry.create(file, newResource.getIdentifier(),
                                                                          UserInstance.fromPublication(
                                                                              newResource.toPublication())))
                                            .map(FileEntry::toDao)
                                            .map(dao -> dao.toPutNewTransactionItem(tableName))
                                            .toList();

        var transactions = new ArrayList<TransactWriteItem>();
        transactions.addAll(Arrays.stream(transactionItems).toList());
        transactions.addAll(fileTransactionWriteItems);
        var transactWriteItemsRequest = new TransactWriteItemsRequest().withTransactItems(transactions);
        sendTransactionWriteRequest(transactWriteItemsRequest);

        return newResource.toImportCandidate();
    }

    private TransactWriteItem[] transactionItemsForNewImportCandidateInsertion(Resource newResource) {
        TransactWriteItem resourceEntry = newPutTransactionItem(new ResourceDao(newResource), tableName);
        TransactWriteItem uniqueIdentifierEntry = createNewTransactionPutEntryForEnsuringUniqueIdentifier(newResource,
                                                                                                          tableName);
        return new TransactWriteItem[]{resourceEntry, uniqueIdentifierEntry};
    }

    private List<TransactWriteItem> transactionItemsForDraftPublicationDeletion(List<Dao> daos)
        throws BadRequestException {
        List<TransactWriteItem> transactionItems = new ArrayList<>();
        transactionItems.addAll(deleteResourceTransactionItems(daos));
        transactionItems.addAll(deleteDoiRequestTransactionItems(daos));
        return transactionItems;
    }

    private List<TransactWriteItem> deleteDoiRequestTransactionItems(List<Dao> daos) {
        Optional<DoiRequestDao> doiRequest = extractDoiRequest(daos);
        if (doiRequest.isPresent()) {
            return deleteDoiRequestTransactionItems(doiRequest.orElseThrow());
        }
        return Collections.emptyList();
    }

    private List<TransactWriteItem> deleteDoiRequestTransactionItems(DoiRequestDao doiRequestDao) {
        WithPrimaryKey identifierEntry = IdentifierEntry.create(doiRequestDao);
        WithPrimaryKey uniqueDoiRequestEntry = UniqueDoiRequestEntry.create(doiRequestDao);
        return Stream.of(doiRequestDao, identifierEntry, uniqueDoiRequestEntry).map(this::newDeleteTransactionItem)

                   .collect(Collectors.toList());
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

    private TransactWriteItem createNewTransactionPutEntryForEnsuringUniqueIdentifier(Resource resource,
                                                                                      String tableName) {
        return newPutTransactionItem(new IdentifierEntry(resource.getIdentifier().toString()), tableName);
    }
}
