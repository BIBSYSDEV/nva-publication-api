package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.DEFAULT_DYNAMODB_CLIENT;
import static no.unit.nva.publication.model.business.Resource.resourceQueryObject;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.core.attempt.Try.attempt;
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
import java.net.URI;
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
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.DeletePublicationStatusResponse;
import no.unit.nva.publication.model.ListingResult;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.ImportCandidate;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.IdentifierEntry;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.model.storage.UniqueDoiRequestEntry;
import no.unit.nva.publication.model.storage.WithPrimaryKey;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;
import nva.commons.core.exceptions.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"PMD.GodClass", "PMD.AvoidDuplicateLiterals"})
public class ResourceService extends ServiceWithTransactions {
    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    public static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_SUPPLIER = SortableIdentifier::next;
    public static final int AWAIT_TIME_BEFORE_FETCH_RETRY = 50;
    public static final String INVALID_PATH_ERROR =
        "The document path provided in the update expression is invalid for update";
    public static final String EMPTY_RESOURCE_IDENTIFIER_ERROR = "Empty resource identifier";

    public static final String DOI_FIELD_IN_RESOURCE = "doi";
    public static final String RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE = "Resource cannot be deleted: ";
    public static final int MAX_SIZE_OF_BATCH_REQUEST = 5;
    public static final String NOT_PUBLISHABLE = "Publication is not publishable. Check main title and doi";
    private final String tableName;
    private final Clock clockForTimestamps;
    private final Supplier<SortableIdentifier> identifierSupplier;
    private final ReadResourceService readResourceService;
    private final UpdateResourceService updateResourceService;
    
    public ResourceService(AmazonDynamoDB client,
                           Clock clock,
                           Supplier<SortableIdentifier> identifierSupplier) {
        super(client);
        tableName = RESOURCES_TABLE_NAME;
        this.clockForTimestamps = clock;
        this.identifierSupplier = identifierSupplier;
        this.readResourceService = new ReadResourceService(client, RESOURCES_TABLE_NAME);
        this.updateResourceService =
            new UpdateResourceService(client, RESOURCES_TABLE_NAME, clockForTimestamps, readResourceService);
    }
    
    public ResourceService(AmazonDynamoDB client, Clock clock) {
        this(client, clock, DEFAULT_IDENTIFIER_SUPPLIER);
    }
    
    @JacocoGenerated
    public static ResourceService defaultService() {
        return new ResourceService(DEFAULT_DYNAMODB_CLIENT, Clock.systemDefaultZone());
    }
    
    public Publication createPublication(UserInstance userInstance, Publication inputData)
        throws BadRequestException {
        Instant currentTime = clockForTimestamps.instant();
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setResourceOwner(createResourceOwner(userInstance));
        newResource.setPublisher(createOrganization(userInstance));
        newResource.setCreatedDate(currentTime);
        newResource.setModifiedDate(currentTime);
        setStatusOnNewPublication(userInstance, inputData, newResource);
        return insertResource(newResource);
    }

    public void setStatusOnNewPublication(UserInstance userInstance,
                                          Publication fromPublication,
                                          Resource toResource) throws BadRequestException {
        var status = userInstance.isExternalClient()
                         ? Optional.ofNullable(fromPublication.getStatus()).orElse(PublicationStatus.DRAFT)
                         : PublicationStatus.DRAFT;

        if (status == PublicationStatus.PUBLISHED && !fromPublication.isPublishable()) {
            throw new BadRequestException(NOT_PUBLISHABLE);
        }

        toResource.setStatus(status);
    }
    
    public Publication createPublicationWithPredefinedCreationDate(Publication inputData) {
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setCreatedDate(inputData.getCreatedDate());
        return insertResource(newResource);
    }
    
    public Publication createPublicationFromImportedEntry(Publication inputData) {
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setPublishedDate(inputData.getPublishedDate());
        newResource.setCreatedDate(inputData.getCreatedDate());
        newResource.setModifiedDate(inputData.getModifiedDate());
        newResource.setStatus(PublicationStatus.PUBLISHED);
        return insertResource(newResource);
    }

    public Publication createImportCandidateFromImportedEntry(ImportCandidate inputData) {
        Resource newResource = Resource.fromImportCandidate(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setPublishedDate(inputData.getPublishedDate());
        newResource.setCreatedDate(inputData.getCreatedDate());
        newResource.setModifiedDate(inputData.getModifiedDate());
        newResource.setStatus(PublicationStatus.PUBLISHED);
        return insertResourceFromImportCandidate(newResource);
    }
    
    public Publication insertPreexistingPublication(Publication publication) {
        Resource resource = Resource.fromPublication(publication);
        return insertResource(resource);
    }
    
    public Publication markPublicationForDeletion(UserInstance userInstance,
                                                  SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return markResourceForDeletion(resourceQueryObject(userInstance, resourceIdentifier)).toPublication();
    }
    
    public PublishPublicationStatusResponse publishPublication(UserInstance userInstance,
                                                               SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return updateResourceService.publishPublication(userInstance, resourceIdentifier);
    }
    
    public void deleteDraftPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws BadRequestException {
        List<Dao> daos = readResourceService
                             .fetchResourceAndDoiRequestFromTheByResourceIndex(userInstance, resourceIdentifier);
        
        List<TransactWriteItem> transactionItems = transactionItemsForDraftPublicationDeletion(daos);
        TransactWriteItemsRequest transactWriteItemsRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(transactWriteItemsRequest);
    }

    public DeletePublicationStatusResponse updatePublishedStatusToDeleted(SortableIdentifier resourceIdentifier)
        throws NotFoundException {
            return updateResourceService.updatePublishedStatusToDeleted(resourceIdentifier);
    }

    public ListingResult<Entity> scanResources(int pageSize, Map<String, AttributeValue> startMarker) {
        var scanRequest = createScanRequestThatFiltersOutIdentityEntries(pageSize, startMarker);
        var scanResult = getClient().scan(scanRequest);
        var values = extractDatabaseEntries(scanResult);
        var isTruncated = thereAreMorePagesToScan(scanResult);
        return new ListingResult<>(values, scanResult.getLastEvaluatedKey(), isTruncated);
    }
    

    public void refreshResources(List<Entity> dataEntries) {
        final var refreshedEntries = refreshAndMigrate(dataEntries);
        var writeRequests = createWriteRequestsForBatchJob(refreshedEntries);
        attempt(() -> writeToDynamoInBatches(writeRequests))
            .orElseThrow(fail -> new BatchUpdateFailureException(collectFailingEntriesIdentifiers(dataEntries)));
    }

    private List<String> collectFailingEntriesIdentifiers(List<Entity> dataEntries) {
        return dataEntries.stream()
                   .map(Entity::getIdentifier)
                   .map(SortableIdentifier::toString)
                   .collect(Collectors.toList());
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

    public List<Publication> getPublicationsByCristinIdentifier(String cristinIdentifier) {
        return  readResourceService.getPublicationsByCristinIdentifier(cristinIdentifier);

    }
    
    public List<Publication> getPublicationsByOwner(UserInstance sampleUser) {
        return readResourceService.getResourcesByOwner(sampleUser);
    }
    
    // TODO rename to getPublicationForUsageWithElevatedRights
    public Publication getPublicationByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return getResourceByIdentifier(identifier).toPublication();
    }

    public ImportCandidate getImportCandidateByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return getResourceByIdentifier(identifier).toImportCandidate();
    }

    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException {
        updateResourceService.updateOwner(identifier, oldOwner, newOwner);
    }
    
    public Publication updatePublication(Publication resourceUpdate) {
        return updateResourceService.updatePublicationButDoNotChangeStatus(resourceUpdate);
    }
    
    // update this method according to current needs.
    //TODO: redesign migration process?
    public Entity migrate(Entity dataEntry) {
        return dataEntry instanceof Resource
                   ? migrateResource((Resource) dataEntry)
                   : migrateOther(dataEntry);
    }
    
    public Stream<TicketEntry> fetchAllTicketsForResource(Resource resource) {
        var dao = (ResourceDao) resource.toDao();
        return dao.fetchAllTickets(getClient())
                   .stream()
                   .map(TicketDao::getData)
                   .map(TicketEntry.class::cast);
    }
    
    public Stream<TicketEntry> fetchAllTicketsForPublication(
        UserInstance userInstance,
        SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        var resource = readResourceService.getResource(userInstance, publicationIdentifier);
        return resource.fetchAllTickets(this);
    }
    
    public Stream<TicketEntry> fetchAllTicketsForElevatedUser(UserInstance userInstance,
                                                              SortableIdentifier publicationIdentifier)
        throws NotFoundException {
        var resource = fetchResourceForElevatedUser(userInstance.getOrganizationUri(), publicationIdentifier);
        return resource.fetchAllTickets(this);
    }

    private Entity migrateOther(Entity dataEntry) {
        if (dataEntry instanceof TicketEntry) {
            var ticket = (TicketEntry) dataEntry;
            var resourceIdentifier = ticket.extractPublicationIdentifier();
            var resource = attempt(() -> getResourceByIdentifier(resourceIdentifier)).orElseThrow();
            ticket.setPublicationDetails(PublicationDetails.create(resource));
            return ticket;
        }
        return dataEntry;
    }
    
    private Resource fetchResourceForElevatedUser(URI customerId, SortableIdentifier publicationIdentifier)
        throws NotFoundException {
        var queryDao = (ResourceDao) Resource.fetchForElevatedUserQueryObject(customerId, publicationIdentifier)
                                         .toDao();
        return (Resource) queryDao.fetchForElevatedUser(getClient()).getData();
    }
    
    private List<Entity> refreshAndMigrate(List<Entity> dataEntries) {
        return dataEntries
                   .stream()
                   .map(attempt(this::migrate))
                   .map(Try::orElseThrow)
                   .collect(Collectors.toList());
    }
    
    private Organization createOrganization(UserInstance userInstance) {
        return new Organization.Builder().withId(userInstance.getOrganizationUri()).build();
    }
    
    private Owner createResourceOwner(UserInstance userInstance) {
        return new Owner(userInstance.getUsername(), userInstance.getTopLevelOrgCristinId());
    }
    
    private boolean thereAreMorePagesToScan(ScanResult scanResult) {
        return nonNull(scanResult.getLastEvaluatedKey()) && !scanResult.getLastEvaluatedKey().isEmpty();
    }
    
    // change this method depending on the current migration needs.
    private Resource migrateResource(Resource dataEntry) {
        return dataEntry;
    }
    
    private Void writeToDynamoInBatches(List<WriteRequest> writeRequests) {
        Lists.partition(writeRequests, MAX_SIZE_OF_BATCH_REQUEST)
            .stream()
            .map(items -> new BatchWriteItemRequest().withRequestItems(Map.of(tableName, items)))
            .forEach(getClient()::batchWriteItem);
        return null;
    }
    
    private List<WriteRequest> createWriteRequestsForBatchJob(List<Entity> refreshedEntries) {
        return refreshedEntries.stream()
                   .map(Entity::toDao)
                   .peek(this::logDao)
                   .map(Dao::toDynamoFormat)
                   .map(item -> new PutRequest().withItem(item))
                   .map(WriteRequest::new)
                   .collect(Collectors.toList());
    }

    private void logDao(Dao dao) {
        logger.info("Refreshing resource {} of type {}", dao.getIdentifier(), dao.getData().getType());
    }

    private ScanRequest createScanRequestThatFiltersOutIdentityEntries(int pageSize,
                                                                       Map<String, AttributeValue> startMarker) {
        return new ScanRequest()
                   .withTableName(tableName)
                   .withIndexName(DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME)
                   .withLimit(pageSize)
                   .withExclusiveStartKey(startMarker)
                   .withFilterExpression(Dao.scanFilterExpressionForDataEntries())
                   .withExpressionAttributeNames(Dao.scanFilterExpressionAttributeNames())
                   .withExpressionAttributeValues(Dao.scanFilterExpressionAttributeValues());
    }
    
    private List<Entity> extractDatabaseEntries(ScanResult response) {
        return response.getItems()
                   .stream()
                   .map(CorrectParsingErrors::apply)
                   .map(value -> parseAttributeValuesMap(value, Dao.class))
                   .map(Dao::getData)
                   .map(Entity.class::cast)
                   .collect(Collectors.toList());
    }
    
    private Publication insertResource(Resource newResource) {
        TransactWriteItem[] transactionItems = transactionItemsForNewResourceInsertion(newResource);
        TransactWriteItemsRequest putRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(putRequest);
        
        return fetchSavedPublication(newResource);
    }

    private Publication insertResourceFromImportCandidate(Resource newResource) {
        TransactWriteItem[] transactionItems = transactionItemsForNewResourceInsertion(newResource);
        TransactWriteItemsRequest putRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(putRequest);

        return fetchSavedImportCandidate(newResource);
    }

    private Publication fetchSavedImportCandidate(Resource newResource) {
        return Optional.ofNullable(fetchSavedResource(newResource))
                .map(Resource::toImportCandidate)
                .orElse(null);
    }

    private Publication fetchSavedPublication(Resource newResource) {
        return Optional.ofNullable(fetchSavedResource(newResource))
                   .map(Resource::toPublication)
                   .orElse(null);
    }
    
    private Resource fetchSavedResource(Resource newResource) {
        return fetchEventualConsistentDataEntry(newResource, readResourceService::getResource)
                   .orElse(null);
    }
    
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
        logger.info("DeleteRequest:{}", request);
        return request;
    }
    
    private Resource sendUpdateRequest(UpdateItemRequest updateRequest) {
        UpdateItemResult requestResult = getClient().updateItem(updateRequest);
        return Try.of(requestResult)
                   .map(UpdateItemResult::getAttributes)
                   .map(valuesMap -> parseAttributeValuesMap(valuesMap, ResourceDao.class))
                   .map(ResourceDao::getData)
                   .map(Resource.class::cast)
                   .orElseThrow();
    }
    
    private TransactWriteItem createNewTransactionPutEntryForEnsuringUniqueIdentifier(Resource resource) {
        return newPutTransactionItem(new IdentifierEntry(resource.getIdentifier().toString()));
    }
}
