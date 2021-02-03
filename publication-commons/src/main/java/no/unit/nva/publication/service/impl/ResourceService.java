package no.unit.nva.publication.service.impl;

import static com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder.S;
import static java.util.Objects.isNull;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.conditionValueMapToAttributeValueMap;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.newTransactWriteItemsRequest;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.parseAttributeValuesMap;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.primaryKeyEqualityConditionAttributeValues;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.toDynamoFormat;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.Resource.resourceQueryObject;
import static nva.commons.core.JsonUtils.objectMapper;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import com.amazonaws.services.dynamodbv2.model.Update;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.xspec.ExpressionSpecBuilder;
import com.amazonaws.services.dynamodbv2.xspec.QueryExpressionSpec;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.FileSet;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.service.impl.exceptions.BadRequestException;
import no.unit.nva.publication.service.impl.exceptions.ResourceCannotBeDeletedException;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.Dao;
import no.unit.nva.publication.storage.model.daos.DoiRequestDao;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;

@SuppressWarnings("PMD.GodClass")
public class ResourceService {

    public static final String EMPTY_STRING = "";
    public static final String DOUBLE_QUOTES = "\"";
    public static final String STATUS_FIELD_IN_RESOURCE = "status";
    public static final String MODIFIED_FIELD_IN_RESOURCE = "modifiedDate";
    public static final String RESOURCE_FIELD_IN_RESOURCE_DAO = ResourceDao.CONTAINED_DATA_FIELD_NAME;
    public static final String RESOURCE_LINK_FIELD = "link";
    public static final String RESOURCE_FILE_SET_FIELD = "fileSet";
    public static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_SUPPLIER = SortableIdentifier::next;
    public static final String RESOURCE_WITHOUT_MAIN_TITLE_ERROR = "Resource is missing main title: ";
    public static final int MAX_FETCH_ATTEMPTS = 10;
    public static final int AWAIT_TIME_BEFORE_FETCH_RETRY = 50;
    public static final String INVALID_PATH_ERROR =
        "The document path provided in the update expression is invalid for update";
    public static final String NOT_FOUND_ERROR_MESSAGE = "The resource could not be found";
    public static final String EMPTY_RESOURCE_IDENTIFIER_ERROR = "Empty resource identifier";
    public static final String DOI_REQUEST_FIELD_IN_DOI_REQUEST_DAO = "data";
    public static final String RESOURCE_STATUS_FIELD_IN_DOI_REQUEST = "resourceStatus";
    public static final String MODIFIED_DATE_FIELD_IN_DOI_REQUEST = "modifiedDate";
    public static final String PUBLISH_COMPLETED = "Publication is published.";
    public static final String PUBLISH_IN_PROGRESS = "Publication is being published. This may take a while.";
    public static final String RAWTYPES = "rawtypes";

    private static final String PUBLISHED_DATE_FIELD_IN_RESOURCE = "publishedDate";
    private static final int RESOURCE_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_EXISTS = 1;
    private static final int RESOURCE_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_NOT_EXISTS = 0;
    private static final int DOI_REQUEST_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_EXISTS = 0;
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

    public Publication createPublication(Publication inputData) throws ConflictException {
        Resource newResource = Resource.fromPublication(inputData);
        newResource.setIdentifier(identifierSupplier.get());
        newResource.setCreatedDate(clockForTimestamps.instant());
        TransactWriteItem[] transactionItems = transactionItemsForNewResourceInsertion(newResource);
        TransactWriteItemsRequest putRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(putRequest);

        return fetchEventuallyConsistentResource(newResource).toPublication();
    }

    public Publication updatePublication(Publication publication) {
        updateResource(Resource.fromPublication(publication));
        return publication;
    }

    public Publication getPublication(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        if (isNull(resourceIdentifier)) {
            throw new BadRequestException(EMPTY_RESOURCE_IDENTIFIER_ERROR);
        }
        return getResource(userInstance, resourceIdentifier)
            .toPublication();
    }

    public Publication getPublication(Publication publication) throws NotFoundException {
        return getResource(Resource.fromPublication(publication)).toPublication();
    }

    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException {
        Resource existingResource = getResource(oldOwner, identifier);
        Resource newResource = updateResourceOwner(newOwner, existingResource);
        TransactWriteItem deleteAction = newDeleteTransactionItem(existingResource);
        TransactWriteItem insertionAction = createTransactionEntyForInsertingResource(newResource);
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(deleteAction, insertionAction);
        client.transactWriteItems(request);
    }

    public List<Publication> getResourcesByOwner(UserInstance userInstance) {
        String partitionKey =
            ResourceDao.constructPrimaryPartitionKey(userInstance.getOrganizationUri(),
                userInstance.getUserIdentifier());
        QueryExpressionSpec querySpec = partitionKeyToQuerySpec(partitionKey);
        Map<String, AttributeValue> valuesMap = conditionValueMapToAttributeValueMap(querySpec.getValueMap(),
            String.class);
        Map<String, String> namesMap = querySpec.getNameMap();
        QueryResult result = performQuery(querySpec.getKeyConditionExpression(), valuesMap, namesMap);

        return queryResultToResourceList(result)
            .stream()
            .map(Resource::toPublication)
            .collect(Collectors.toList());
    }

    public PublishPublicationStatusResponse publishPublication(UserInstance userInstance,
                                                               SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        return publishResource(userInstance, resourceIdentifier);
    }

    public Publication markPublicationForDeletion(UserInstance userInstance,
                                                  SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {

        return markResourceForDeletion(resourceQueryObject(userInstance, resourceIdentifier))
            .toPublication();
    }

    private static List<Resource> queryResultToResourceList(QueryResult result) {
        return result.getItems()
            .stream()
            .map(resultValuesMap -> parseAttributeValuesMap(resultValuesMap, ResourceDao.class))
            .map(ResourceDao::getData)
            .collect(Collectors.toList());
    }

    private void updateResource(Resource resourceUpdate) {

        ResourceDao resourceDao = new ResourceDao(resourceUpdate);

        Map<String, AttributeValue> primaryKeyConditionAttributeValues =
            primaryKeyEqualityConditionAttributeValues(resourceDao);

        PutItemRequest putItemRequest = new PutItemRequest()
            .withItem(toDynamoFormat(resourceDao))
            .withTableName(tableName)
            .withConditionExpression(PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION)
            .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
            .withExpressionAttributeValues(primaryKeyConditionAttributeValues);

        client.putItem(putItemRequest);
    }

    private Resource markResourceForDeletion(Resource resource)
        throws ApiGatewayException {
        ResourceDao dao = new ResourceDao(resource);
        UpdateItemRequest updateRequest = markForDeletionUpdateRequest(dao);
        return attempt(() -> sendUpdateRequest(updateRequest))
            .orElseThrow(failure -> markForDeletionError(failure, resource));
    }

    private Resource fetchEventuallyConsistentResource(Resource newResource) {
        Resource savedResource = null;
        for (int times = 0; times < MAX_FETCH_ATTEMPTS && savedResource == null; times++) {
            savedResource = attempt(() -> getResource(newResource)).orElse(fail -> null);
            attempt(this::waitBeforeFetching).orElseThrow();
        }
        return savedResource;
    }

    private Void waitBeforeFetching() throws InterruptedException {
        Thread.sleep(AWAIT_TIME_BEFORE_FETCH_RETRY);
        return null;
    }

    private Resource getResource(UserInstance userInstance, SortableIdentifier identifier) throws NotFoundException {
        return getResource(resourceQueryObject(userInstance, identifier));
    }

    private Resource getResource(Resource resource) throws NotFoundException {
        Map<String, AttributeValue> primaryKey = new ResourceDao(resource).primaryKey();
        GetItemResult getResult = getResourceByPrimaryKey(primaryKey);
        ResourceDao fetchedDao = parseAttributeValuesMap(getResult.getItem(), ResourceDao.class);
        return fetchedDao.getData();
    }

    @SuppressWarnings(RAWTYPES)
    private PublishPublicationStatusResponse publishResource(UserInstance userInstance,
                                                             SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        List<Dao> daos = fetchResourceAndDoiRequestFromTheByResourceIndex(userInstance, resourceIdentifier);
        ResourceDao resourceDao = extractResourceDao(daos);

        if (PublicationStatus.PUBLISHED.equals(resourceDao.getData().getStatus())) {
            return publishCompletedStatus();
        }

        validateForPublishing(resourceDao.getData());
        setResourceStatusToPublished(daos, resourceDao);
        return publishingInProgressStatus();
    }

    private void setResourceStatusToPublished(List<Dao> daos, ResourceDao resourceDao) {
        List<TransactWriteItem> transactionItems = createUpdateTransactionItems(daos, resourceDao);

        TransactWriteItemsRequest transactWriteItemsRequest = new TransactWriteItemsRequest()
            .withTransactItems(transactionItems);
        client.transactWriteItems(transactWriteItemsRequest);
    }

    private List<TransactWriteItem> createUpdateTransactionItems(List<Dao> daos, ResourceDao resourceDao) {
        String nowString = nowAsString();
        List<TransactWriteItem> transactionItems = new ArrayList<>();
        transactionItems.add(publishUpdateRequest(resourceDao, nowString));
        Optional<DoiRequestDao> doiRequestDao = extractDoiRequest(daos);
        doiRequestDao.ifPresent(dao -> transactionItems.add(updateDoiRequestResourceStatus(dao, nowString)));
        return transactionItems;
    }

    private PublishPublicationStatusResponse publishingInProgressStatus() {
        return new PublishPublicationStatusResponse(PUBLISH_IN_PROGRESS, HttpURLConnection.HTTP_ACCEPTED);
    }

    private PublishPublicationStatusResponse publishCompletedStatus() {
        return new PublishPublicationStatusResponse(PUBLISH_COMPLETED, HttpURLConnection.HTTP_NO_CONTENT);
    }

    private TransactWriteItem publishUpdateRequest(ResourceDao dao, String nowString) {

        dao.getData().setStatus(PublicationStatus.PUBLISHED);
        final String updateExpression = "SET"
                                        + " #data.#status = :newStatus, "
                                        + "#data.#modifiedDate = :modifiedDate, "
                                        + "#data.#publishedDate = :modifiedDate, "
                                        + "#PK1 = :PK1, "
                                        + "#SK1 = :SK1 ";

        final String conditionExpression = "#data.#status <> :publishedStatus";

        Map<String, String> expressionNamesMap = Map.of(
            "#data", RESOURCE_FIELD_IN_RESOURCE_DAO,
            "#status", STATUS_FIELD_IN_RESOURCE,
            "#modifiedDate", MODIFIED_FIELD_IN_RESOURCE,
            "#publishedDate", PUBLISHED_DATE_FIELD_IN_RESOURCE,
            "#PK1", DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME,
            "#SK1", DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME
        );

        Map<String, AttributeValue> expressionValuesMap = Map.of(
            ":newStatus", new AttributeValue(PublicationStatus.PUBLISHED.getValue()),
            ":modifiedDate", new AttributeValue(nowString),
            ":publishedStatus", new AttributeValue(PublicationStatus.PUBLISHED.getValue()),
            ":PK1", new AttributeValue(dao.getByTypeCustomerStatusPartitionKey()),
            ":SK1", new AttributeValue(dao.getByTypeCustomerStatusSortKey()));

        Update update = new Update()
            .withTableName(tableName)
            .withKey(dao.primaryKey())
            .withUpdateExpression(updateExpression)
            .withConditionExpression(conditionExpression)
            .withExpressionAttributeNames(expressionNamesMap)
            .withExpressionAttributeValues(expressionValuesMap);
        return new TransactWriteItem().withUpdate(update);
    }

    private TransactWriteItem updateDoiRequestResourceStatus(DoiRequestDao doiRequestDao, String nowString) {

        String updateExpression = "SET "
                                  + "#data.#resourceStatus = :publishedStatus,"
                                  + "#data.#modifiedDate = :modifiedDate ";
        Map<String, String> expressionAttributeNames = Map.of(
            "#data", DOI_REQUEST_FIELD_IN_DOI_REQUEST_DAO,
            "#resourceStatus", RESOURCE_STATUS_FIELD_IN_DOI_REQUEST,
            "#modifiedDate", MODIFIED_DATE_FIELD_IN_DOI_REQUEST
        );
        Map<String, AttributeValue> attributeValueMap = Map.of(
            ":publishedStatus", new AttributeValue(PublicationStatus.PUBLISHED.getValue()),
            ":modifiedDate", new AttributeValue(nowString)
        );

        Update update = new Update().withTableName(tableName)
            .withKey(doiRequestDao.primaryKey())
            .withUpdateExpression(updateExpression)
            .withExpressionAttributeNames(expressionAttributeNames)
            .withExpressionAttributeValues(attributeValueMap);

        return new TransactWriteItem().withUpdate(update);
    }

    @SuppressWarnings(RAWTYPES)
    private ResourceDao extractResourceDao(List<Dao> daos) throws BadRequestException {
        if (doiRequestExists(daos)) {
            return (ResourceDao) daos.get(RESOURCE_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_EXISTS);
        } else if (onlyResourceExisits(daos)) {
            return (ResourceDao) daos.get(RESOURCE_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_NOT_EXISTS);
        }
        throw new BadRequestException(RESOURCE_NOT_FOUND_MESSAGE);
    }

    @SuppressWarnings(RAWTYPES)
    private boolean onlyResourceExisits(List<Dao> daos) {
        return daos.size() == 1;
    }

    @SuppressWarnings(RAWTYPES)
    private boolean doiRequestExists(List<Dao> daos) {
        return daos.size() == 2;
    }

    @SuppressWarnings(RAWTYPES)
    private Optional<DoiRequestDao> extractDoiRequest(List<Dao> daos) {
        if (doiRequestExists(daos)) {
            return Optional.of((DoiRequestDao) daos.get(DOI_REQUEST_INDEX_IN_QUERY_RESULT_WHEN_DOI_REQUEST_EXISTS));
        }
        return Optional.empty();
    }

    @SuppressWarnings(RAWTYPES)
    private List<Dao> fetchResourceAndDoiRequestFromTheByResourceIndex(UserInstance userInstance,
                                                                       SortableIdentifier resourceIdentifier) {
        ResourceDao queryObject = ResourceDao.queryObject(userInstance, resourceIdentifier);
        QueryRequest queryRequest = queryByResourceIndex(queryObject);
        QueryResult queryResult = client.query(queryRequest);
        return parseResultSetToDaos(queryResult);
    }

    @SuppressWarnings(RAWTYPES)
    private List<Dao> parseResultSetToDaos(QueryResult queryResult) {
        return queryResult.getItems()
            .stream()
            .map(values -> parseAttributeValuesMap(values, Dao.class))
            .collect(Collectors.toList());
    }

    private QueryRequest queryByResourceIndex(ResourceDao queryObject) {
        Map<String, Condition> keyConditions = queryObject
            .byResource(
                DoiRequestDao.joinByResourceContainedOrderedType(),
                ResourceDao.joinByResourceContainedOrderedType()
            );
        return new QueryRequest()
            .withTableName(tableName)
            .withIndexName(BY_RESOURCE_INDEX_NAME)
            .withKeyConditions(keyConditions);
    }

    private <E extends Exception> ApiGatewayException markForDeletionError(
        Failure<Resource> failure, Resource resource) {
        if (primaryKeyConditionFailed(failure.getException())) {
            return new NotFoundException(NOT_FOUND_ERROR_MESSAGE);
        } else if (failure.getException() instanceof ConditionalCheckFailedException) {
            return new ResourceCannotBeDeletedException(resource.getIdentifier().toString());
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

    private void validateForPublishing(Resource resource) throws InvalidPublicationException {
        boolean hasNoTitle = Optional.of(resource)
            .map(Resource::getEntityDescription)
            .map(EntityDescription::getMainTitle)
            .isEmpty();
        boolean hasNeitherLinkNorFile = isNull(resource.getLink()) && emptyResourceFiles(resource);

        if (hasNoTitle) {
            throwErrorWhenPublishingResourceWithoutMainTitle(resource);
        } else if (hasNeitherLinkNorFile) {
            throwErrorWhenPublishingResourceWithoutData(resource);
        }
    }

    private void throwErrorWhenPublishingResourceWithoutData(Resource resource) throws InvalidPublicationException {
        String linkField = attempt(() -> findFieldNameOrThrowError(resource, RESOURCE_LINK_FIELD)).orElseThrow();
        String files = attempt(() -> findFieldNameOrThrowError(resource, RESOURCE_FILE_SET_FIELD)).orElseThrow();
        throw new InvalidPublicationException(List.of(files, linkField));
    }

    private void throwErrorWhenPublishingResourceWithoutMainTitle(Resource resource)
        throws InvalidPublicationException {
        throw new
            InvalidPublicationException(RESOURCE_WITHOUT_MAIN_TITLE_ERROR + resource.getIdentifier().toString());
    }

    private String findFieldNameOrThrowError(Resource resource, String resourceField) throws NoSuchFieldException {
        return resource.getClass().getDeclaredField(resourceField).getName();
    }

    private boolean emptyResourceFiles(Resource resource) {
        return Optional.ofNullable(resource.getFileSet())
            .map(FileSet::getFiles)
            .map(List::isEmpty)
            .orElse(true);
    }

    private Resource sendUpdateRequest(UpdateItemRequest updateRequest) {
        UpdateItemResult requestResult = client.updateItem(updateRequest);
        return Try.of(requestResult)
            .map(UpdateItemResult::getAttributes)
            .map(valuesMap -> parseAttributeValuesMap(valuesMap, ResourceDao.class))
            .map(ResourceDao::getData)
            .orElseThrow();
    }



    private String nowAsString() {
        String jsonString = attempt(() -> objectMapper.writeValueAsString(clockForTimestamps.instant()))
            .orElseThrow();
        return jsonString.replaceAll(DOUBLE_QUOTES, EMPTY_STRING);
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
        return existingResource
            .copy()
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
        TransactWriteItem resourceEntry = createTransactionEntyForInsertingResource(resource);
        TransactWriteItem uniqueIdentifierEntry = createNewTransactionPutEntryForEnsuringUniqueIdentifier(resource);
        return new TransactWriteItem[]{resourceEntry, uniqueIdentifierEntry};
    }

    private TransactWriteItem createTransactionEntyForInsertingResource(Resource resource) {
        return createTransactionPutEntry(new ResourceDao(resource));
    }

    private <T extends WithPrimaryKey> TransactWriteItem createTransactionPutEntry(T resourceDao) {
        return ResourceServiceUtils.createTransactionPutEntry(resourceDao, tableName);
    }

    private TransactWriteItem createNewTransactionPutEntryForEnsuringUniqueIdentifier(Resource resource) {
        return createTransactionPutEntry(new IdentifierEntry(resource.getIdentifier().toString()));
    }

    private ConflictException handleTransactionFailure(Failure<TransactWriteItemsResult> fail) {
        return new ConflictException(fail.getException());
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
