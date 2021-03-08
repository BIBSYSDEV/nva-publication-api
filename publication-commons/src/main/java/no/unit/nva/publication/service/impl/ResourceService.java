package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.Resource.resourceQueryObject;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.BadRequestException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
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
    
    private final String tableName;
    private final AmazonDynamoDB client;
    private final Clock clockForTimestamps;
    private final Supplier<SortableIdentifier> identifierSupplier;
    private final ReadResourceService readResourceService;
    private final UpdateResourceService updateResourceService;
    
    public ResourceService(AmazonDynamoDB client, Clock clock, Supplier<SortableIdentifier> identifierSupplier) {
        super();
        tableName = RESOURCES_TABLE_NAME;
        this.client = client;
        this.clockForTimestamps = clock;
        this.identifierSupplier = identifierSupplier;
        this.readResourceService = new ReadResourceService(client, RESOURCES_TABLE_NAME);
        this.updateResourceService =
            new UpdateResourceService(client, RESOURCES_TABLE_NAME, clockForTimestamps, readResourceService);
    }

    public ResourceService(AmazonDynamoDB client, Clock clock) {
        this(client, clock, DEFAULT_IDENTIFIER_SUPPLIER);
    }

    public Publication createPublication(Publication inputData) throws TransactionFailedException {
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setCreatedDate(clockForTimestamps.instant());
        return insertResource(newResource);
    }

    public Publication insertPreexistingPublication(Publication publication)
        throws TransactionFailedException {
        Resource resource = Resource.fromPublication(publication);
        return insertResource(resource);
    }

    private Publication insertResource(Resource newResource) throws TransactionFailedException {
        TransactWriteItem[] transactionItems = transactionItemsForNewResourceInsertion(newResource);
        TransactWriteItemsRequest putRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(putRequest);

        return fetchSavedResource(newResource);
    }

    public PublishPublicationStatusResponse publishPublication(UserInstance userInstance,
                                                               SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return updateResourceService.publishResource(userInstance, resourceIdentifier);
    }
    
    public Publication markPublicationForDeletion(UserInstance userInstance,
                                                  SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        
        return markResourceForDeletion(resourceQueryObject(userInstance, resourceIdentifier))
                   .toPublication();
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
    
    public Publication getPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return readResourceService.getPublication(userInstance, resourceIdentifier);
    }
    
    public Publication getPublication(Publication sampleResource) throws NotFoundException {
        return readResourceService.getPublication(sampleResource);
    }
    
    public List<Publication> getPublicationsByOwner(UserInstance sampleUser) {
        return readResourceService.getResourcesByOwner(sampleUser);
    }
    
    public Publication getPublicationByIdentifier(SortableIdentifier identifier) throws NotFoundException {
        return readResourceService.getPublicationByIdentifier(identifier);
    }
    
    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException, TransactionFailedException {
        updateResourceService.updateOwner(identifier, oldOwner, newOwner);
    }
    
    public Publication updatePublication(Publication resourceUpdate) throws TransactionFailedException {
        return updateResourceService.updatePublication(resourceUpdate);
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
        
        return new UpdateItemRequest()
                   .withTableName(tableName)
                   .withKey(dao.primaryKey())
                   .withUpdateExpression(updateExpression)
                   .withConditionExpression(conditionExpression)
                   .withExpressionAttributeNames(expressionAttributeNames)
                   .withExpressionAttributeValues(expressionValuesMap)
                   .withReturnValues(ReturnValue.ALL_NEW);
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
