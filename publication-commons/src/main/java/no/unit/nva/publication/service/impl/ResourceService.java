package no.unit.nva.publication.service.impl;

import static java.util.Objects.nonNull;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.publication.model.business.Resource.resourceQueryObject;
import static no.unit.nva.publication.model.storage.DynamoEntry.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ReadResourceService.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutRequest;
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
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.importcandidate.ImportCandidate;
import no.unit.nva.publication.model.business.importcandidate.ImportStatus;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.IdentifierEntry;
import no.unit.nva.publication.model.storage.KeyField;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.model.storage.UniqueDoiRequestEntry;
import no.unit.nva.publication.model.storage.WithPrimaryKey;
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

@SuppressWarnings({"PMD.GodClass", "PMD.AvoidDuplicateLiterals"})
public class ResourceService extends ServiceWithTransactions {

    public static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_SUPPLIER = SortableIdentifier::next;
    public static final int AWAIT_TIME_BEFORE_FETCH_RETRY = 50;
    public static final String RESOURCE_REFRESHED_MESSAGE = "Resource has been refreshed successfully: {}";
    public static final String INVALID_PATH_ERROR = "The document path provided in the update expression is invalid "
                                                    + "for update";
    public static final String EMPTY_RESOURCE_IDENTIFIER_ERROR = "Empty resource identifier";
    public static final String DOI_FIELD_IN_RESOURCE = "doi";
    public static final String RESOURCE_CANNOT_BE_DELETED_ERROR_MESSAGE = "Resource cannot be deleted: ";
    public static final int MAX_SIZE_OF_BATCH_REQUEST = 5;
    public static final String NOT_PUBLISHABLE = "Publication is not publishable. Check main title and doi";
    public static final String IMPORT_CANDIDATE_HAS_BEEN_DELETED_MESSAGE = "Import candidate has been deleted: ";
    public static final String ONLY_PUBLISHED_PUBLICATIONS_CAN_BE_UNPUBLISHED_ERROR_MESSAGE = "Only published "
                                                                                              + "publications can be "
                                                                                              + "unpublished";
    public static final String DELETE_PUBLICATION_ERROR_MESSAGE = "Only unpublished publication can be deleted";
    private static final String SEPARATOR_ITEM = ",";
    private static final String SEPARATOR_TABLE = ";";
    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);
    public static final String COULD_NOT_REFRESH_RESOURCE = "Could not refresh resource with identifier: {}";
    public static final String RESOURCE_TO_REFRESH_NOT_FOUND_MESSAGE = "Resource to refresh is not found: {}";
    private final String tableName;
    private final Clock clockForTimestamps;
    private final Supplier<SortableIdentifier> identifierSupplier;
    private final ReadResourceService readResourceService;
    private final UpdateResourceService updateResourceService;
    private final DeleteResourceService deleteResourceService;

    protected ResourceService(AmazonDynamoDB dynamoDBClient, String tableName, Clock clock,
                              Supplier<SortableIdentifier> identifierSupplier) {
        super(dynamoDBClient);
        this.tableName = tableName;
        this.clockForTimestamps = clock;
        this.identifierSupplier = identifierSupplier;
        this.readResourceService = new ReadResourceService(client, this.tableName);
        this.updateResourceService = new UpdateResourceService(client, this.tableName, clockForTimestamps,
                                                               readResourceService);
        this.deleteResourceService = new DeleteResourceService(client, this.tableName, readResourceService);
    }

    @JacocoGenerated
    public static ResourceService defaultService() {
        return builder().build();
    }

    /**
     * Should not be used initiating resourceService for resource-table
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
        setStatusOnNewPublication(userInstance, inputData, newResource);
        return insertResource(newResource);
    }

    @JacocoGenerated
    public void setStatusOnNewPublication(UserInstance userInstance, Publication fromPublication, Resource toResource)
        throws BadRequestException {
        var status = userInstance.isExternalClient() ? Optional.ofNullable(fromPublication.getStatus())
                                                           .orElse(PublicationStatus.DRAFT) : PublicationStatus.DRAFT;

        if (status == PUBLISHED && !fromPublication.isPublishable()) {
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
        newResource.setStatus(PUBLISHED);
        return insertResource(newResource);
    }

    /**
     * Persists importCandidate with updated database metadata fields.
     *
     * @param inputData importCandidate from external source
     * @return updated importCandidate that has been sent to persistence
     */
    public ImportCandidate persistImportCandidate(ImportCandidate inputData) {
        Resource newResource = Resource.fromImportCandidate(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setPublishedDate(inputData.getPublishedDate());
        newResource.setCreatedDate(inputData.getCreatedDate());
        newResource.setModifiedDate(inputData.getModifiedDate());
        return insertResourceFromImportCandidate(newResource);
    }

    public Publication insertPreexistingPublication(Publication publication) {
        Resource resource = Resource.fromPublication(publication);
        return insertResource(resource);
    }

    public Publication markPublicationForDeletion(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return markResourceForDeletion(resourceQueryObject(userInstance, resourceIdentifier)).toPublication();
    }

    public PublishPublicationStatusResponse publishPublication(UserInstance userInstance,
                                                               SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return updateResourceService.publishPublication(userInstance, resourceIdentifier);
    }

    public Publication autoImportPublication(ImportCandidate inputData) {
        var publication = inputData.toPublication();
        Instant currentTime = clockForTimestamps.instant();
        var userInstance = UserInstance.fromPublication(publication);
        Resource newResource = Resource.fromPublication(publication);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setResourceOwner(createResourceOwner(userInstance));
        newResource.setPublisher(createOrganization(userInstance));
        newResource.setCreatedDate(currentTime);
        newResource.setModifiedDate(currentTime);
        newResource.setPublishedDate(currentTime);
        newResource.setStatus(PUBLISHED);
        return insertResource(newResource);
    }

    public void deleteDraftPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws BadRequestException {
        List<Dao> daos = readResourceService.fetchResourceAndDoiRequestFromTheByResourceIndex(userInstance,
                                                                                              resourceIdentifier);

        List<TransactWriteItem> transactionItems = transactionItemsForDraftPublicationDeletion(daos);
        TransactWriteItemsRequest transactWriteItemsRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(transactWriteItemsRequest);
    }

    public DeletePublicationStatusResponse updatePublishedStatusToDeleted(SortableIdentifier resourceIdentifier)
        throws NotFoundException {
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

    @JacocoGenerated
    public void refreshResources(List<Entity> dataEntries) {
        final var refreshedEntries = refreshAndMigrate(dataEntries);
        var writeRequests = createWriteRequestsForBatchJob(refreshedEntries);
        writeToDynamoInBatches(writeRequests);
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
        return readResourceService.getPublicationsByCristinIdentifier(cristinIdentifier);
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

    public ImportCandidate updateImportStatus(SortableIdentifier identifier, ImportStatus status)
        throws NotFoundException {
        return updateResourceService.updateStatus(identifier, status);
    }

    public void deleteImportCandidate(ImportCandidate importCandidate) throws BadMethodException, NotFoundException {
        deleteResourceService.deleteImportCandidate(importCandidate);
        logger.info(IMPORT_CANDIDATE_HAS_BEEN_DELETED_MESSAGE + importCandidate.getIdentifier());
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
        return dataEntry instanceof Resource ? migrateResource((Resource) dataEntry) : dataEntry;
    }

    public Stream<TicketEntry> fetchAllTicketsForResource(Resource resource) {
        var dao = (ResourceDao) resource.toDao();
        return dao.fetchAllTickets(getClient())
                   .stream()
                   .map(TicketDao::getData)
                   .map(TicketEntry.class::cast)
                   .filter(ResourceService::isNotRemoved);
    }

    public Stream<TicketEntry> fetchAllTicketsForPublication(UserInstance userInstance,
                                                             SortableIdentifier publicationIdentifier)
        throws ApiGatewayException {
        var resource = readResourceService.getResource(userInstance, publicationIdentifier);
        return resource.fetchAllTickets(this).filter(ResourceService::isNotRemoved);
    }

    public void refresh(SortableIdentifier identifier) {
        try {
            updatePublication(getPublicationByIdentifier(identifier));
            logger.info(RESOURCE_REFRESHED_MESSAGE, identifier);
        } catch (NotFoundException e) {
            logger.error(RESOURCE_TO_REFRESH_NOT_FOUND_MESSAGE, identifier);
        }
    }

    public Stream<TicketEntry> fetchAllTicketsForElevatedUser(UserInstance userInstance,
                                                              SortableIdentifier publicationIdentifier)
        throws NotFoundException {
        var resource = fetchResourceForElevatedUser(userInstance.getCustomerId(), publicationIdentifier);
        return resource.fetchAllTickets(this);
    }

    public ImportCandidate updateImportCandidate(ImportCandidate importCandidate) throws BadRequestException {
        return updateResourceService.updateImportCandidate(importCandidate);
    }

    public void unpublishPublication(Publication publication) throws BadRequestException {
        if (!PUBLISHED.equals(publication.getStatus())) {
            throw new BadRequestException(ONLY_PUBLISHED_PUBLICATIONS_CAN_BE_UNPUBLISHED_ERROR_MESSAGE);
        }
        var allTicketsForResource = fetchAllTicketsForResource(Resource.fromPublication(publication));
        var unpublishRequestTicket = (UnpublishRequest) UnpublishRequest.fromPublication(publication);
        updateResourceService.unpublishPublication(publication, allTicketsForResource, unpublishRequestTicket);
    }

    public void deletePublication(Publication publication) throws BadRequestException {
        if (!UNPUBLISHED.equals(publication.getStatus())) {
            throw new BadRequestException(DELETE_PUBLICATION_ERROR_MESSAGE);
        }
        updateResourceService.deletePublication(publication);
    }

    private static boolean isNotRemoved(TicketEntry ticket) {
        return !TicketStatus.REMOVED.equals(ticket.getStatus());
    }

    private Resource fetchResourceForElevatedUser(URI customerId, SortableIdentifier publicationIdentifier)
        throws NotFoundException {
        var queryDao = (ResourceDao) Resource.fetchForElevatedUserQueryObject(customerId, publicationIdentifier)
                                         .toDao();
        return (Resource) queryDao.fetchForElevatedUser(getClient()).getData();
    }

    private List<Entity> refreshAndMigrate(List<Entity> dataEntries) {
        return dataEntries.stream().map(attempt(this::migrate)).map(Try::orElseThrow).collect(Collectors.toList());
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

    // change this method depending on the current migration needs.
    private Resource migrateResource(Resource dataEntry) {
        return dataEntry;
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

    private ImportCandidate insertResourceFromImportCandidate(Resource newResource) {
        TransactWriteItem[] transactionItems = transactionItemsForNewImportCandidateInsertion(newResource);
        TransactWriteItemsRequest putRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(putRequest);

        return fetchSavedImportCandidate(newResource);
    }

    private TransactWriteItem[] transactionItemsForNewImportCandidateInsertion(Resource newResource) {
        TransactWriteItem resourceEntry = newPutTransactionItem(new ResourceDao(newResource), tableName);
        TransactWriteItem uniqueIdentifierEntry = createNewTransactionPutEntryForEnsuringUniqueIdentifier(newResource,
                                                                                                          tableName);
        return new TransactWriteItem[]{resourceEntry, uniqueIdentifierEntry};
    }

    private ImportCandidate fetchSavedImportCandidate(Resource newResource) {
        return Optional.ofNullable(fetchSavedResource(newResource)).map(Resource::toImportCandidate).orElse(null);
    }

    private Publication fetchSavedPublication(Resource newResource) {
        return Optional.ofNullable(fetchSavedResource(newResource)).map(Resource::toPublication).orElse(null);
    }

    private Resource fetchSavedResource(Resource newResource) {
        return fetchEventualConsistentDataEntry(newResource, readResourceService::getResource).orElse(null);
    }

    private List<TransactWriteItem> transactionItemsForDraftPublicationDeletion(List<Dao> daos)
        throws BadRequestException {
        List<TransactWriteItem> transactionItems = new ArrayList<>();
        transactionItems.addAll(deleteResourceTransactionItems(daos));
        transactionItems.addAll(deleteDoiRequestTransactionItems(daos));
        return transactionItems;
    }

    private TransactWriteItem[] transactionItemsForNewResourceInsertion(Resource resource) {
        TransactWriteItem resourceEntry = newPutTransactionItem(new ResourceDao(resource), tableName);
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
        Map<String, String> expressionAttributeNames = Map.of("#status", STATUS_FIELD_IN_RESOURCE, "#doi",
                                                              DOI_FIELD_IN_RESOURCE);
        Map<String, AttributeValue> expressionAttributeValues = Map.of(":publishedStatus",
                                                                       new AttributeValue(PUBLISHED.getValue()));

        deleteResource.getDelete()
            .withConditionExpression("#status <> :publishedStatus AND attribute_not_exists(#doi)")
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
