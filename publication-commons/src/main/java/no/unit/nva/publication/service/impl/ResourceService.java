package no.unit.nva.publication.service.impl;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static java.util.Objects.isNull;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_PLACEHOLDERS_AND_ATTRIBUTE_NAMES_MAPPING;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.conditionValueMapToAttributeValueMap;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.newPutTransactionItem;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.newTransactWriteItemsRequest;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.toDynamoFormat;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.valueMapForKeyConditionCheck;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.utils.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.impl.exceptions.ResourceCannotBeDeletedException;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;
import nva.commons.exceptions.commonexceptions.ConflictException;
import nva.commons.exceptions.commonexceptions.NotFoundException;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.attempt.Failure;
import nva.commons.utils.attempt.Try;

@SuppressWarnings("PMD.GodClass")
public class ResourceService {

    public static final String EMPTY_STRING = "";
    public static final String DOUBLE_QUOTES = "\"";
    public static final String STATUS_FIELD_IN_RESOURCE = "status";
    public static final String MODIFIED_FIELD_IN_RESOURCE = "modifiedDate";
    public static final String RESOURCE_FIELD_IN_RESOURCE_DAO = "resource";
    private static final String PUBLISHED_DATE_FIELD_IN_RESOURCE = "publishedDate";
    public static final String RESOURCE_MAIN_TITLE_FIELD = "title";
    public static final String RESOURCE_LINK_FIELD = "link";
    public static final String RESOURCE_FILES_FIELD = "files";
    public static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_SUPPLIER = SortableIdentifier::next;
    private final String tableName;
    private final AmazonDynamoDB client;
    private final Clock clockForTimestamps;
    private final Supplier<SortableIdentifier> identifierSupplier;

    public ResourceService(AmazonDynamoDB client, Clock clock, Supplier<SortableIdentifier> identifierSupplier) {
        tableName = RESOURCES_TABLE_NAME;
        this.client = client;
        this.clockForTimestamps = clock;
        this.identifierSupplier = identifierSupplier;
    }

    public ResourceService() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone(), DEFAULT_IDENTIFIER_SUPPLIER);
    }

    public ResourceService(AmazonDynamoDB client, Clock clock) {
        this(client, clock, DEFAULT_IDENTIFIER_SUPPLIER);
    }

    public Resource createResource(Resource inputData) throws ConflictException {

        Resource newResource = inputData.copy().withIdentifier(identifierSupplier.get()).build();
        newResource.setCreatedDate(clockForTimestamps.instant());
        TransactWriteItem[] transactionItems = transactionItemsForNewResourceInsertion(newResource);
        TransactWriteItemsRequest putRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(putRequest);

        return fetchEventuallyConsistentResource(newResource);
    }

    private Resource fetchEventuallyConsistentResource(Resource newResource) {
        Resource savedResource = null;
        for (int times = 0; times < 10 && savedResource == null; times++) {
            savedResource = attempt(() -> getResource(newResource)).orElse(fail -> null);
        }
        return savedResource;
    }

    public void updateResource(Resource resourceUpdate) {
        ResourceDao resourceDao = new ResourceDao(resourceUpdate);

        Map<String, AttributeValue> primaryKeyCheckValuesMap = valueMapForKeyConditionCheck(resourceDao);

        PutItemRequest putItemRequest = new PutItemRequest()
            .withItem(toDynamoFormat(resourceDao))
            .withTableName(tableName)
            .withConditionExpression(PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION)
            .withExpressionAttributeNames(PRIMARY_KEY_PLACEHOLDERS_AND_ATTRIBUTE_NAMES_MAPPING)
            .withExpressionAttributeValues(primaryKeyCheckValuesMap);

        client.putItem(putItemRequest);
    }

    public Resource getResource(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws NotFoundException {
        return getResource(createQueryObject(userInstance, resourceIdentifier.toString()));
    }

    public Resource getResource(UserInstance userInstance, String resourceIdentifier)
        throws NotFoundException {
        return getResource(createQueryObject(userInstance, resourceIdentifier));
    }

    public Resource getResource(Resource resource) throws NotFoundException {
        Map<String, AttributeValue> primaryKey = new ResourceDao(resource).primaryKey();
        GetItemResult getResult = getResourceByPrimaryKey(primaryKey);
        ResourceDao fetchedDao = parseAttributeValuesMap(getResult.getItem(), ResourceDao.class);
        return fetchedDao.getResource();
    }

    public void updateOwner(String identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException {
        Resource existingResource = getResource(oldOwner, identifier);
        Resource newResource = updateResourceOwner(newOwner, existingResource);
        TransactWriteItem deleteAction = newDeleteTransactionItem(existingResource);
        TransactWriteItem insertionAction = newPutTransactionItem(createNewTransactionPutDataEntry(newResource));
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(deleteAction, insertionAction);
        client.transactWriteItems(request);
    }

    public List<Resource> getResourcesByOwner(UserInstance userInstance) {
        String partitionKey =
            ResourceDao.formatPrimaryPartitionKey(userInstance.getOrganizationUri(), userInstance.getUserIdentifier());
        QueryExpressionSpec querySpec = partitionKeyToQuerySpec(partitionKey);
        Map<String, AttributeValue> valuesMap = conditionValueMapToAttributeValueMap(querySpec.getValueMap(),
            String.class);
        Map<String, String> namesMap = querySpec.getNameMap();
        QueryResult result = performQuery(querySpec.getKeyConditionExpression(), valuesMap, namesMap);

        return queryResultToResourceList(result);
    }

    public Resource publishResource(Resource resource)
        throws JsonProcessingException, NotFoundException, InvalidPublicationException {
        Resource existingResource = getResource(resource);
        if (PublicationStatus.PUBLISHED.equals(existingResource.getStatus())) {
            return existingResource;
        }
        validateForPublishing(resource);
        ResourceDao dao = new ResourceDao(resource);
        UpdateItemRequest updateRequest = publishUpdateRequest(dao);
        return sendUpdateRequest(updateRequest);
    }

    public Resource markPublicationForDeletion(Resource resource)
        throws JsonProcessingException, ResourceCannotBeDeletedException {
        ResourceDao dao = new ResourceDao(resource);
        UpdateItemRequest updateRequest = markForDeletionUpdateRequest(dao);
        return attempt(() -> sendUpdateRequest(updateRequest))
            .orElseThrow(failure -> handleConditionFailureException(failure, resource));
    }

    private <E extends Exception> ResourceCannotBeDeletedException handleConditionFailureException(
        Failure<Resource> failure, Resource resource) {
        if (failure.getException() instanceof ConditionalCheckFailedException) {
            return new ResourceCannotBeDeletedException(resource.getIdentifier().toString());
        }
        throw new RuntimeException(failure.getException());
    }

    private UpdateItemRequest markForDeletionUpdateRequest(ResourceDao dao) throws JsonProcessingException {
        String updateExpression = "SET "
                                  + "#resource.#status = :status, "
                                  + "#resource.#modifiedDate = :modifiedDate";
        String conditionExpression = "#resource.#status <> :publishedStatus";

        ConcurrentHashMap<String, AttributeValue> expressionValuesMap =
            updateStatusExpresionValuesMap(PublicationStatus.DRAFT_FOR_DELETION);
        expressionValuesMap.put(":publishedStatus",
            new AttributeValue(PublicationStatus.PUBLISHED.toString()));

        return new UpdateItemRequest()
            .withTableName(tableName)
            .withKey(dao.primaryKey())
            .withUpdateExpression(updateExpression)
            .withConditionExpression(conditionExpression)
            .withExpressionAttributeNames(updateStatusNamesExpressionMap())
            .withExpressionAttributeValues(expressionValuesMap)
            .withReturnValues(ReturnValue.ALL_NEW);
    }

    private void validateForPublishing(Resource resource) throws InvalidPublicationException {
        boolean hasNoTitle = isNull(resource.getTitle());
        boolean hasNeitherLinkNorFile = isNull(resource.getLink()) && emptyResourceFiles(resource);

        if (hasNoTitle) {
            String missingField = attempt(() -> findFieldNameOrThrowError(resource, RESOURCE_MAIN_TITLE_FIELD))
                .orElseThrow();
            throw new InvalidPublicationException(Collections.singletonList(missingField));
        } else if (hasNeitherLinkNorFile) {
            String linkField = attempt(() -> findFieldNameOrThrowError(resource, RESOURCE_LINK_FIELD)).orElseThrow();
            String files = attempt(() -> findFieldNameOrThrowError(resource, RESOURCE_FILES_FIELD)).orElseThrow();
            throw new InvalidPublicationException(List.of(files, linkField));
        }
    }

    private String findFieldNameOrThrowError(Resource resource, String resourceField) throws NoSuchFieldException {
        return resource.getClass().getDeclaredField(resourceField).getName();
    }

    private boolean emptyResourceFiles(Resource resource) {
        return Optional.ofNullable(resource.getFiles())
            .map(FileSet::getFiles)
            .map(List::isEmpty)
            .orElse(true);
    }

    private Resource sendUpdateRequest(UpdateItemRequest updateRequest) {
        UpdateItemResult requestResult = client.updateItem(updateRequest);
        return Try.of(requestResult)
            .map(UpdateItemResult::getAttributes)
            .map(valuesMap -> parseAttributeValuesMap(valuesMap, ResourceDao.class))
            .map(ResourceDao::getResource)
            .orElseThrow();
    }

    private UpdateItemRequest publishUpdateRequest(ResourceDao dao) throws JsonProcessingException {

        ConcurrentHashMap<String, String> expressionNamesMap = publishUpdateExpressionNamesMap();
        ConcurrentHashMap<String, AttributeValue> expressionValuesMap =
            updateStatusExpresionValuesMap(PublicationStatus.PUBLISHED);

        String updateExpression = "SET"
                                  + " #resource.#status = :status, "
                                  + "#resource.#modifiedDate = :modifiedDate, "
                                  + "#resource.#publishedDate = :modifiedDate ";
        return new UpdateItemRequest()
            .withTableName(tableName)
            .withKey(dao.primaryKey())
            .withUpdateExpression(updateExpression)
            .withConditionExpression("#resource.#status <> " + PublicationStatus.PUBLISHED.toString())
            .withExpressionAttributeNames(expressionNamesMap)
            .withExpressionAttributeValues(expressionValuesMap)
            .withReturnValues(ReturnValue.ALL_NEW);
    }

    private ConcurrentHashMap<String, String> publishUpdateExpressionNamesMap() {
        ConcurrentHashMap<String, String> expressionNamesMap = updateStatusNamesExpressionMap();
        expressionNamesMap.put("#publishedDate", PUBLISHED_DATE_FIELD_IN_RESOURCE);
        return expressionNamesMap;
    }

    private ConcurrentHashMap<String, String> updateStatusNamesExpressionMap() {
        ConcurrentHashMap<String, String> expressionNamesMap = new ConcurrentHashMap<>();
        expressionNamesMap.put("#status", STATUS_FIELD_IN_RESOURCE);
        expressionNamesMap.put("#modifiedDate", MODIFIED_FIELD_IN_RESOURCE);
        expressionNamesMap.put("#resource", RESOURCE_FIELD_IN_RESOURCE_DAO);
        return expressionNamesMap;
    }

    private ConcurrentHashMap<String, AttributeValue> updateStatusExpresionValuesMap(
        PublicationStatus publicationStatus)
        throws JsonProcessingException {
        String nowString = instantAsString(clockForTimestamps.instant());
        ConcurrentHashMap<String, AttributeValue> expressionValuesMap = new ConcurrentHashMap<>();
        expressionValuesMap.put(":status", new AttributeValue(publicationStatus.getValue()));
        expressionValuesMap.put(":modifiedDate", new AttributeValue(nowString));
        return expressionValuesMap;
    }

    private String instantAsString(Instant instant) throws JsonProcessingException {
        String jsonString = JsonUtils.objectMapper.writeValueAsString(instant);
        return jsonString.replaceAll(DOUBLE_QUOTES, EMPTY_STRING);
    }

    private static List<Resource> queryResultToResourceList(QueryResult result) {
        return result.getItems()
            .stream()
            .map(resultValuesMap -> parseAttributeValuesMap(resultValuesMap, ResourceDao.class))
            .map(ResourceDao::getResource)
            .collect(Collectors.toList());
    }

    private QueryResult performQuery(String conditionExpression, Map<String, AttributeValue> valuesMap,
                                     Map<String, String> namesMap) {
        return client.query(
            new QueryRequest().withKeyConditionExpression(conditionExpression)
                .withExpressionAttributeNames(namesMap)
                .withExpressionAttributeValues(valuesMap)
                .withTableName(tableName)
        );
    }

    private QueryExpressionSpec partitionKeyToQuerySpec(String partitionKey) {
        return new ExpressionSpecBuilder()
            .withKeyCondition(S(PRIMARY_KEY_PARTITION_KEY_NAME).eq(partitionKey)).buildForQuery();
    }

    private TransactWriteItemsResult sendTransactionWriteRequest(TransactWriteItemsRequest putRequest)
        throws ConflictException {
        return attempt(() -> client.transactWriteItems(putRequest)).orElseThrow(this::handleTransactionFailure);
    }

    private Resource updateResourceOwner(UserInstance newOwner, Resource existingResource) {
        return existingResource.copy()
            .withPublisher(userOrganization(newOwner))
            .withOwner(newOwner.getUserIdentifier())
            .withModifiedDate(clockForTimestamps.instant())
            .build();
    }

    private <T extends WithPrimaryKey> TransactWriteItem newDeleteTransactionItem(Resource resource) {
        ResourceDao resourceDao = new ResourceDao(resource);
        return new TransactWriteItem()
            .withDelete(new Delete().withTableName(tableName).withKey(resourceDao.primaryKey()));
    }

    private TransactWriteItem[] transactionItemsForNewResourceInsertion(Resource resource) {
        Put resourceEntry = createNewTransactionPutDataEntry(resource);
        Put uniqueIdentifierEntry = createNewTransactionPutEntryForEnsuringUniqueIdentifier(resource);

        TransactWriteItem dataEntry = newPutTransactionItem(resourceEntry);
        TransactWriteItem identifierEntry = newPutTransactionItem(uniqueIdentifierEntry);

        return new TransactWriteItem[]{dataEntry, identifierEntry};
    }

    private Put createNewTransactionPutDataEntry(Resource resource) {
        return createTransactionPutEntry(new ResourceDao(resource));
    }

    private Put createNewTransactionPutEntryForEnsuringUniqueIdentifier(Resource resource) {
        return createTransactionPutEntry(new IdentifierEntry(resource.getIdentifier().toString()));
    }

    private ConflictException handleTransactionFailure(Failure<TransactWriteItemsResult> fail) {
        return new ConflictException(fail.getException());
    }

    private Resource createQueryObject(UserInstance userInstance, String resourceIdentifier) {
        return Resource.emptyResource(userInstance.getUserIdentifier(), userInstance.getOrganizationUri(),
            resourceIdentifier);
    }

    private <T extends WithPrimaryKey> Put createTransactionPutEntry(T data) {
        return new Put().withItem(toDynamoFormat(data)).withTableName(tableName)
            .withConditionExpression(KEY_EXISTS_CONDITION)
            .withExpressionAttributeNames(PRIMARY_KEY_PLACEHOLDERS_AND_ATTRIBUTE_NAMES_MAPPING);
    }

    private GetItemResult getResourceByPrimaryKey(Map<String, AttributeValue> primaryKey) throws NotFoundException {
        GetItemResult result = client.getItem(new GetItemRequest()
            .withTableName(tableName)
            .withKey(primaryKey));
        if (isNull(result.getItem())) {
            throw new NotFoundException(RESOURCE_NOT_FOUND_MESSAGE);
        }
        return result;
    }
}
