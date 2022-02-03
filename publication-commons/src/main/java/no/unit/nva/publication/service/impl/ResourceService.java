package no.unit.nva.publication.service.impl;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.google.common.collect.Lists;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.DoiRequestDao;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.publication.storage.model.daos.UniqueDoiRequestEntry;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.Resource.resourceQueryObject;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static nva.commons.core.attempt.Try.attempt;

@SuppressWarnings({"PMD.GodClass", "PMD.AvoidDuplicateLiterals"})
public class ResourceService extends ServiceWithTransactions {

    public static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_SUPPLIER = SortableIdentifier::next;
    public static final int MAX_FETCH_ATTEMPTS = 10;
    public static final int AWAIT_TIME_BEFORE_FETCH_RETRY = 50;
    public static final String INVALID_PATH_ERROR =
        "The document path provided in the update expression is invalid for update";
    public static final String EMPTY_RESOURCE_IDENTIFIER_ERROR = "Empty resource identifier";

    public static final String DOI_FIELD_IN_RESOURCE = "doi";
    public static final String RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE = "Resource cannot be deleted: ";
    public static final int MAX_SIZE_OF_BATCH_REQUEST = 20;
    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);
    private final String tableName;
    private final AmazonDynamoDB client;
    private final Clock clockForTimestamps;
    private final Supplier<SortableIdentifier> identifierSupplier;
    private final ReadResourceService readResourceService;
    private final UpdateResourceService updateResourceService;
    private final AffiliationSelectionService affiliationService;

    public ResourceService(AmazonDynamoDB client,
                           HttpClient externalServicesHttpClient,
                           Clock clock,
                           Supplier<SortableIdentifier> identifierSupplier) {
        super();
        tableName = RESOURCES_TABLE_NAME;
        this.client = client;
        this.clockForTimestamps = clock;
        this.identifierSupplier = identifierSupplier;
        this.readResourceService = new ReadResourceService(client, RESOURCES_TABLE_NAME);
        this.updateResourceService =
            new UpdateResourceService(client, RESOURCES_TABLE_NAME, clockForTimestamps, readResourceService);
        this.affiliationService = AffiliationSelectionService.create(externalServicesHttpClient);
    }

    public ResourceService(AmazonDynamoDB client,
                           HttpClient externalServicesHttpClient,
                           Clock clock) {
        this(client, externalServicesHttpClient, clock, DEFAULT_IDENTIFIER_SUPPLIER);
    }

    public Publication createPublication(UserInstance userInstance, Publication inputData)
        throws ApiGatewayException {
        Instant currentTime = clockForTimestamps.instant();
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setResourceOwner(createResourceOwner(userInstance));
        newResource.setPublisher(createOrganization(userInstance));
        newResource.setCreatedDate(currentTime);
        newResource.setModifiedDate(currentTime);
        newResource.setStatus(PublicationStatus.DRAFT);
        return insertResource(newResource);
    }

    public Publication createPublicationWithPredefinedCreationDate(Publication inputData)
        throws TransactionFailedException {
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setCreatedDate(inputData.getCreatedDate());
        return insertResource(newResource);
    }

    public Publication createPublicationWhilePersistingEntryFromLegacySystems(Publication inputData)
        throws TransactionFailedException {
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setPublishedDate(inputData.getPublishedDate());
        newResource.setCreatedDate(inputData.getCreatedDate());
        newResource.setModifiedDate(inputData.getModifiedDate());
        newResource.setStatus(PublicationStatus.PUBLISHED);
        return insertResource(newResource);
    }

    public Publication insertPreexistingPublication(Publication publication)
        throws TransactionFailedException {
        Resource resource = Resource.fromPublication(publication);
        return insertResource(resource);
    }

    public Publication markPublicationForDeletion(UserInstance userInstance,
                                                  SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {

        return markResourceForDeletion(resourceQueryObject(userInstance, resourceIdentifier))
            .toPublication();
    }

    public PublishPublicationStatusResponse publishPublication(UserInstance userInstance,
                                                               SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return updateResourceService.publishResource(userInstance, resourceIdentifier);
    }

    @SuppressWarnings(RAWTYPES)
    public void deleteDraftPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws BadRequestException, TransactionFailedException {
        List<Dao> daos = readResourceService
            .fetchResourceAndDoiRequestFromTheByResourceIndex(userInstance, resourceIdentifier);

        List<TransactWriteItem> transactionItems = transactionItemsForDraftPublicationDeletion(daos);
        TransactWriteItemsRequest transactWriteItemsRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(transactWriteItemsRequest);
    }

    public ListingResult<DataEntry> scanResources(int pageSize, Map<String, AttributeValue> startMarker) {
        var scanRequest = createScanRequestThatFiltersOutIdentityEntries(pageSize, startMarker);
        var scanResult = client.scan(scanRequest);
        var values = extractDatabaseEntries(scanResult);
        var isTruncated = thereAreMorePagesToScan(scanResult);
        return new ListingResult<>(values, scanResult.getLastEvaluatedKey(), isTruncated);
    }

    public List<DataEntry> refreshResources(List<DataEntry> dataEntries) {
        final var refreshedEntries = refreshAndMigrate(dataEntries);
        var writeRequests = createWriteRequestsForBatchJob(refreshedEntries);
        writeToS3InBatches(writeRequests);
        return refreshedEntries;
    }

    public Publication getPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return readResourceService.getPublication(userInstance, resourceIdentifier);
    }

    public Publication getPublication(Publication sampleResource) throws NotFoundException {
        return readResourceService.getPublication(sampleResource);
    }

    public Resource getResourceByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return readResourceService.getResourceByIdentifier(identifier);
    }

    public List<Publication> getPublicationsByOwner(UserInstance sampleUser) {
        return readResourceService.getResourcesByOwner(sampleUser);
    }

    public Publication getPublicationByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return getResourceByIdentifier(identifier).toPublication();
    }

    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException, TransactionFailedException {
        updateResourceService.updateOwner(identifier, oldOwner, newOwner);
    }

    public Publication updatePublication(Publication resourceUpdate) throws TransactionFailedException {
        return updateResourceService.updatePublication(resourceUpdate);
    }

    // update this method according to current needs.
    public DataEntry migrate(DataEntry dataEntry) throws ApiGatewayException {
        return dataEntry instanceof Resource
                   ? migrateResource((Resource) dataEntry)
                   : dataEntry;
    }

    @Override
    protected String getTableName() {
        return tableName;
    }

    @Override
    protected AmazonDynamoDB getClient() {
        return client;
    }

    @Override
    protected Clock getClock() {
        return clockForTimestamps;
    }

    private List<DataEntry> refreshAndMigrate(List<DataEntry> dataEntries) {
        return dataEntries
            .stream()
            .map(attempt(this::migrate))
            .map(Try::orElseThrow)
            .map(DataEntry::refreshRowVersion)
            .collect(Collectors.toList());
    }

    private Organization createOrganization(UserInstance userInstance) {
        return new Organization.Builder().withId(userInstance.getOrganizationUri()).build();
    }

    private ResourceOwner createResourceOwner(UserInstance userInstance)
        throws ApiGatewayException {
        return this.affiliationService.fetchAffiliation(userInstance.getUserIdentifier())
            .map(affiliation -> new ResourceOwner(userInstance.getUserIdentifier(), affiliation))
            .orElse(new ResourceOwner(userInstance.getUserIdentifier(), null));
    }

    private boolean thereAreMorePagesToScan(ScanResult scanResult) {
        return nonNull(scanResult.getLastEvaluatedKey()) && !scanResult.getLastEvaluatedKey().isEmpty();
    }

    // change this method depending on the current migration needs.
    private Resource migrateResource(Resource dataEntry) throws ApiGatewayException {
        var userInstance = new UserInstance(dataEntry.getOwner(), dataEntry.getPublisher().getId());
        var resourceOwner = createResourceOwner(userInstance);
        dataEntry.setResourceOwner(resourceOwner);
        return dataEntry;
    }

    private void writeToS3InBatches(List<WriteRequest> writeRequests) {
        Lists.partition(writeRequests, MAX_SIZE_OF_BATCH_REQUEST)
            .stream()
            .map(items -> new BatchWriteItemRequest().withRequestItems(Map.of(tableName, items)))
            .forEach(client::batchWriteItem);
    }

    private List<WriteRequest> createWriteRequestsForBatchJob(List<DataEntry> refreshedEntries) {
        return refreshedEntries.stream()
            .map(DataEntry::toDao)
            .map(Dao::toDynamoFormat)
            .map(item -> new PutRequest().withItem(item))
            .map(WriteRequest::new)
            .collect(Collectors.toList());
    }

    private ScanRequest createScanRequestThatFiltersOutIdentityEntries(int pageSize,
                                                                       Map<String, AttributeValue> startMarker) {
        return new ScanRequest()
            .withTableName(tableName)
            .withIndexName(DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME)
            .withLimit(pageSize)
            .withExclusiveStartKey(startMarker)
            .withFilterExpression(Dao.scanFilterExpression())
            .withExpressionAttributeNames(Dao.scanFilterExpressionAttributeNames())
            .withExpressionAttributeValues(Dao.scanFilterExpressionAttributeValues());
    }


    private List<DataEntry> extractDatabaseEntries(ScanResult response) {return response.getItems()
            .stream()
            .map(CorrectParsingErrors::apply)
            .map(value -> parseAttributeValuesMap(value, Dao.class))
            .map(Dao::getData)
            .map(DataEntry.class::cast)
            .collect(Collectors.toList());
    }

    private Publication insertResource(Resource newResource) throws TransactionFailedException {
        TransactWriteItem[] transactionItems = transactionItemsForNewResourceInsertion(newResource);
        TransactWriteItemsRequest putRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(putRequest);

        return fetchSavedResource(newResource);
    }

    private Publication fetchSavedResource(Resource newResource) {
        return fetchEventuallyConsistentResource(newResource)
            .map(Resource::toPublication)
            .orElse(null);
    }

    @SuppressWarnings(RAWTYPES)
    private List<TransactWriteItem> transactionItemsForDraftPublicationDeletion(List<Dao> daos)
        throws BadRequestException {
        List<TransactWriteItem> transactionItems = new ArrayList<>();
        transactionItems.addAll(deleteResourceTransactionItems(daos));
        transactionItems.addAll(deleteDoiRequestTransactionItems(daos));
        return transactionItems;
    }

    private TransactWriteItem[] transactionItemsForNewResourceInsertion(Resource resource) {
        TransactWriteItem resourceEntry = newPutTransactionItem(new ResourceDao(resource));
        TransactWriteItem uniqueIdentifierEntry = createNewTransactionPutEntryForEnsuringUniqueIdentifier(resource);
        return new TransactWriteItem[]{resourceEntry, uniqueIdentifierEntry};
    }

    @SuppressWarnings(RAWTYPES)
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
        return
            Stream.of(doiRequestDao, identifierEntry, uniqueDoiRequestEntry)
                .map(this::newDeleteTransactionItem)

                .collect(Collectors.toList());
    }

    @SuppressWarnings(RAWTYPES)
    private List<TransactWriteItem> deleteResourceTransactionItems(List<Dao> daos)
        throws BadRequestException {
        ResourceDao resourceDao = extractResourceDao(daos);

        TransactWriteItem deleteResourceItem = newDeleteTransactionItem(resourceDao);
        applyDeleteResourceConditions(deleteResourceItem);

        TransactWriteItem deleteResourceIdentifierItem = newDeleteTransactionItem(IdentifierEntry.create(resourceDao));

        return List.of(deleteResourceItem, deleteResourceIdentifierItem);
    }

    private void applyDeleteResourceConditions(TransactWriteItem deleteResource) {
        Map<String, String> expressionAttributeNames = Map.of(
            "#data", RESOURCE_FIELD_IN_RESOURCE_DAO,
            "#status", STATUS_FIELD_IN_RESOURCE,
            "#doi", DOI_FIELD_IN_RESOURCE
        );
        Map<String, AttributeValue> expressionAttributeValues = Map.of(
            ":publishedStatus", new AttributeValue(PublicationStatus.PUBLISHED.getValue())
        );

        deleteResource.getDelete()
            .withConditionExpression("#data.#status <> :publishedStatus AND attribute_not_exists(#data.#doi)")
            .withExpressionAttributeNames(expressionAttributeNames)
            .withExpressionAttributeValues(expressionAttributeValues);
    }

    private Resource markResourceForDeletion(Resource resource)
        throws ApiGatewayException {
        ResourceDao dao = new ResourceDao(resource);
        UpdateItemRequest updateRequest = markForDeletionUpdateRequest(dao);
        return attempt(() -> sendUpdateRequest(updateRequest))
            .orElseThrow(failure -> markForDeletionError(failure, resource));
    }

    private Optional<Resource> fetchEventuallyConsistentResource(Resource newResource) {
        Resource savedResource = null;
        for (int times = 0; times < MAX_FETCH_ATTEMPTS && savedResource == null; times++) {
            savedResource = attempt(() -> readResourceService.getResource(newResource)).orElse(fail -> null);
            attempt(this::waitBeforeFetching).orElseThrow();
        }
        return Optional.ofNullable(savedResource);
    }

    private Void waitBeforeFetching() throws InterruptedException {
        Thread.sleep(AWAIT_TIME_BEFORE_FETCH_RETRY);
        return null;
    }

    private ApiGatewayException markForDeletionError(Failure<Resource> failure, Resource resource) {
        if (primaryKeyConditionFailed(failure.getException())) {
            return new NotFoundException(ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE);
        } else if (failure.getException() instanceof ConditionalCheckFailedException) {
            logger.warn(ExceptionUtils.stackTraceInSingleLine(failure.getException()));
            return new BadRequestException(RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE
                                           + resource.getIdentifier().toString());
        }
        throw new RuntimeException(failure.getException());
    }

    private boolean primaryKeyConditionFailed(Exception exception) {
        return exception instanceof AmazonServiceException
               && messageRefersToInvalidPath(exception);
    }

    private boolean messageRefersToInvalidPath(Exception exception) {
        return exception.getMessage().contains(INVALID_PATH_ERROR);
    }

    private UpdateItemRequest markForDeletionUpdateRequest(ResourceDao dao) {
        String updateExpression = "SET "
                                  + "#data.#status = :newStatus, "
                                  + "#data.#modifiedDate = :modifiedDate";

        String conditionExpression = "#data.#status = :expectedExistingStatus";

        Map<String, AttributeValue> expressionValuesMap = Map.of(
            ":newStatus", new AttributeValue(PublicationStatus.DRAFT_FOR_DELETION.getValue()),
            ":modifiedDate", new AttributeValue(nowAsString()),
            ":expectedExistingStatus", new AttributeValue(PublicationStatus.DRAFT.toString())
        );

        Map<String, String> expressionAttributeNames = Map.of(
            "#status", STATUS_FIELD_IN_RESOURCE,
            "#modifiedDate", MODIFIED_FIELD_IN_RESOURCE,
            "#data", RESOURCE_FIELD_IN_RESOURCE_DAO);

        UpdateItemRequest request = new UpdateItemRequest()
            .withTableName(tableName)
            .withKey(dao.primaryKey())
            .withUpdateExpression(updateExpression)
            .withConditionExpression(conditionExpression)
            .withExpressionAttributeNames(expressionAttributeNames)
            .withExpressionAttributeValues(expressionValuesMap)
            .withReturnValues(ReturnValue.ALL_NEW);
        logger.info("DeleteRequest:" + request.toString());
        return request;
    }

    private Resource sendUpdateRequest(UpdateItemRequest updateRequest) {
        UpdateItemResult requestResult = client.updateItem(updateRequest);
        return Try.of(requestResult)
            .map(UpdateItemResult::getAttributes)
            .map(valuesMap -> parseAttributeValuesMap(valuesMap, ResourceDao.class))
            .map(ResourceDao::getData)
            .orElseThrow();
    }

    private TransactWriteItem createNewTransactionPutEntryForEnsuringUniqueIdentifier(Resource resource) {
        return newPutTransactionItem(new IdentifierEntry(resource.getIdentifier().toString()));
    }
}
