package no.unit.nva.publication.service.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.exception.DynamoDBException;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.PublishingRequest;
import no.unit.nva.publication.storage.model.PublishingRequestStatus;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.PublishingRequestDao;
import no.unit.nva.publication.storage.model.daos.UniquePublicationRequestEntry;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.SingletonCollector;
import nva.commons.core.attempt.Failure;
import nva.commons.core.attempt.Try;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static nva.commons.core.attempt.Try.attempt;

public class PublishingRequestService extends ServiceWithTransactions {

    public static final String PUBLISHING_REQUEST_NOT_FOUND_FOR_RESOURCE =
        "Could not find a Publishing Request for Resource: ";
    public static final String ALREADY_PUBLISHED_OR_DELETED_AND_CANNOT_BE_PUBLISHED =
        "Publication is already published or deleted, and cannot be published";
    public static final String UPDATE_PUBLISHING_REQUEST_STATUS_CONDITION_FAILURE_MESSAGE =
            "Could not update Publishing Request status.";


    private static final int DEFAULT_QUERY_RESULT_SIZE = 10_000;

    private final AmazonDynamoDB client;
    private final Clock clock;
    private final String tableName;
    private final Supplier<SortableIdentifier> identifierProvider;
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;
    private final ReadResourceService resourceService;

    public PublishingRequestService(AmazonDynamoDB client, Clock clock) {
        this(client, clock, DEFAULT_IDENTIFIER_PROVIDER);
    }

    protected PublishingRequestService(AmazonDynamoDB client,
                                       Clock clock,
                                       Supplier<SortableIdentifier> identifierProvider) {
        super();
        this.client = client;
        this.clock = clock;
        this.tableName = RESOURCES_TABLE_NAME;
        this.identifierProvider = identifierProvider;
        resourceService = new ReadResourceService(client, tableName);
    }

    public void createPublishingRequest(UserInstance user, SortableIdentifier publicationIdentifier) throws ApiGatewayException {
        createPublishingRequest(getPublication(user, publicationIdentifier));
    }

    private Publication getPublication(UserInstance user, SortableIdentifier publicationIdentifier) throws ApiGatewayException {
        return resourceService.getPublication(user, publicationIdentifier);
    }

    private void createPublishingRequest(Publication publication) throws BadRequestException, TransactionFailedException {
        verifyPublicationIsPublishable(publication);
        var publishingRequest = createNewPublishingRequestEntry(publication);
        var request = createInsertionTransactionRequest(publishingRequest);
        sendTransactionWriteRequest(request);
    }

    public PublishingRequest getPublishingRequest(UserInstance resourceOwner,
                                                  SortableIdentifier resourceIdentifier)
            throws NotFoundException {
        var queryResult = getQueryResult(resourceOwner, resourceIdentifier);

        var item = parseQueryResultExpectingSingleItem(queryResult)
                .orElseThrow(fail -> handleFetchPublishingRequestByResoureError(resourceIdentifier));
        return parseAttributeValuesMap(item, PublishingRequestDao.class).getData();
    }

    private QueryResult getQueryResult(UserInstance resourceOwner, SortableIdentifier resourceIdentifier) {
        var queryObject = PublishingRequestDao.queryByCustomerAndResourceIdentifier(resourceOwner,
                resourceIdentifier);
        var queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                .withKeyConditions(queryObject.byResource(PublishingRequestDao.joinByResourceContainedOrderedType()));
        return client.query(queryRequest);
    }

    public List<PublishingRequest> listPublishingRequestsForUser(UserInstance userInstance) {
        var query = listPublishingRequestForUserQuery(userInstance, DEFAULT_QUERY_RESULT_SIZE);
        return performQueryWithPotentiallyManyResults(query);
    }

    public void updatePublishingRequest(UserInstance userInstance,
                                        SortableIdentifier resourceIdentifier,
                                        PublishingRequestStatus status) throws ApiGatewayException {

        var updateItemRequest = createRequestForUpdatingPublishingRequest(userInstance, resourceIdentifier, status);

        var item = attempt(() -> client.updateItem(updateItemRequest))
                .orElseThrow(this::handleUpdatePublishingRequestFailure);
        parseAttributeValuesMap(item.getAttributes(), PublishingRequestDao.class);
    }

    private void verifyPublicationIsPublishable(Publication publication) throws BadRequestException {
        if (publication.getStatus() == PublicationStatus.PUBLISHED
                || publication.getStatus() == PublicationStatus.DRAFT_FOR_DELETION) {
            throw new BadRequestException(ALREADY_PUBLISHED_OR_DELETED_AND_CANNOT_BE_PUBLISHED);
        }
    }

    private PublishingRequest createNewPublishingRequestEntry(Publication publication) {
        var resource = Resource.fromPublication(publication);
        return PublishingRequest.newPublishingRequestResource(identifierProvider.get(), resource, clock.instant());
    }

    private TransactWriteItemsRequest createInsertionTransactionRequest(PublishingRequest publicationRequest) {
        var publicationRequestEntry = createPublishingRequestInsertionEntry(publicationRequest);
        var identifierEntry = createUniqueIdentifierEntry(publicationRequest);
        var uniquePublicationRequestEntry = createUniquePublishingRequestEntry(publicationRequest);

        return new TransactWriteItemsRequest()
                .withTransactItems(
                        identifierEntry,
                        uniquePublicationRequestEntry,
                        publicationRequestEntry);
    }

    private TransactWriteItem createPublishingRequestInsertionEntry(PublishingRequest publicationRequest) {
        return newPutTransactionItem(new PublishingRequestDao(publicationRequest));
    }

    private TransactWriteItem createUniquePublishingRequestEntry(PublishingRequest publicationRequest) {
        var uniquePublicationRequestEntry = new UniquePublicationRequestEntry(
                publicationRequest.getResourceIdentifier().toString());
        return newPutTransactionItem(uniquePublicationRequestEntry);
    }

    private TransactWriteItem createUniqueIdentifierEntry(PublishingRequest publicationRequest) {
        var identifierEntry = new IdentifierEntry(publicationRequest.getIdentifier().toString());
        return newPutTransactionItem(identifierEntry);
    }

    private QueryRequest listPublishingRequestForUserQuery(UserInstance userInstance, int maxResultSize) {
        var queryExpression = "#PK= :PK";
        var filterExpression = "#data.#status = :pendingStatus OR #data.#status = :rejectedStatus";

        var expressionAttributeNames =
                Map.of(
                        "#PK", DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME,
                        "#data", PublishingRequestDao.CONTAINED_DATA_FIELD_NAME,
                        "#status", PublishingRequest.STATUS_FIELD);

        var primaryKeyPartitionKeyValue = publishingRequestPrimaryKeyPartionKeyValue(userInstance);

        var expressionAttributeValues = Map.of(
                ":PK", new AttributeValue(primaryKeyPartitionKeyValue),
                ":pendingStatus", new AttributeValue(PublishingRequestStatus.PENDING.toString()),
                ":rejectedStatus", new AttributeValue(PublishingRequestStatus.REJECTED.toString())
        );

        return new QueryRequest()
                .withTableName(tableName)
                .withKeyConditionExpression(queryExpression)
                .withFilterExpression(filterExpression)
                .withExpressionAttributeNames(expressionAttributeNames)
                .withExpressionAttributeValues(expressionAttributeValues)
                .withLimit(maxResultSize);
    }

    protected UpdateItemRequest createRequestForUpdatingPublishingRequest(UserInstance userInstance,
                                                                          SortableIdentifier resourceIdentifier,
                                                                          PublishingRequestStatus status) throws NotFoundException {
        var now = nowAsString();

        var dao = createUpdatedPublishingRequestDao(userInstance, resourceIdentifier, status);
        var updateExpression = "SET"
                + "#data.#status = :status, "
                + "#data.#modifiedDate = :modifiedDate,"
                + "#PK1 = :PK1 ,"
                + "#SK1 = :SK1 ,"
                + "#PK2 = :PK2 ,"
                + "#SK2 = :SK2 ";

        var conditionExpression = "#data.#resourceStatus <> :publishedStatus";

        var expressionAttributeNames = Map.of(
                "#data", PublishingRequestDao.CONTAINED_DATA_FIELD_NAME,
                "#status", PublishingRequest.STATUS_FIELD,
                "#modifiedDate", PublishingRequest.MODIFIED_DATE_FIELD,
                "#resourceStatus", PublishingRequest.RESOURCE_STATUS_FIELD,
                "#PK1", BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME,
                "#SK1", BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME,
                "#PK2", BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME,
                "#SK2", BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME

        );

        var expressionAttributeValues = Map.of(
                ":status", new AttributeValue(dao.getData().getStatus().name()),
                ":modifiedDate", new AttributeValue(now),
                ":publishedStatus", new AttributeValue(PublicationStatus.PUBLISHED.toString()),
                ":PK1", new AttributeValue(dao.getByTypeCustomerStatusPartitionKey()),
                ":SK1", new AttributeValue(dao.getByTypeCustomerStatusSortKey()),
                ":PK2", new AttributeValue(dao.getByCustomerAndResourcePartitionKey()),
                ":SK2", new AttributeValue(dao.getByCustomerAndResourceSortKey())
        );
        return new UpdateItemRequest()
                .withTableName(tableName)
                .withKey(dao.primaryKey())
                .withUpdateExpression(updateExpression)
                .withConditionExpression(conditionExpression)
                .withExpressionAttributeNames(expressionAttributeNames)
                .withExpressionAttributeValues(expressionAttributeValues)
                .withReturnValues(ReturnValue.ALL_NEW);
    }

    private PublishingRequestDao createUpdatedPublishingRequestDao(UserInstance userInstance, SortableIdentifier resourceIdentifier,
                                                                   PublishingRequestStatus status) throws NotFoundException {
        var publicationRequest = getPublishingRequest(userInstance, resourceIdentifier);
        var existingStatus = publicationRequest.getStatus();
        publicationRequest.setStatus(existingStatus.changeStatus(status));
        return new PublishingRequestDao(publicationRequest);
    }

    private String publishingRequestPrimaryKeyPartionKeyValue(UserInstance userInstance) {
        var queryObject = PublishingRequestDao.queryObject(userInstance.getOrganizationUri(),
                userInstance.getUserIdentifier());
        return queryObject.getPrimaryKeyPartitionKey();
    }

    private static Try<Map<String, AttributeValue>> parseQueryResultExpectingSingleItem(QueryResult queryResult) {
        return attempt(() -> queryResult.getItems()
                .stream()
                .collect(SingletonCollector.collect()));
    }

    private List<PublishingRequest> performQueryWithPotentiallyManyResults(QueryRequest query) {
        Map<String, AttributeValue> startKey = null;
        var result = new ArrayList<PublishingRequest>();
        do {
            query.withExclusiveStartKey(startKey);
            var queryResult = client.query(query);
            var lastItems = parseListingPublishingRequestsQueryResult(queryResult);
            result.addAll(lastItems);
            startKey = queryResult.getLastEvaluatedKey();
        } while (hasMoreResults(startKey));
        return result;
    }

    private List<PublishingRequest> parseListingPublishingRequestsQueryResult(QueryResult result) {
        return result.getItems()
                .stream()
                .map(map -> parseAttributeValuesMap(map, PublishingRequestDao.class))
                .map(PublishingRequestDao::getData)
                .collect(Collectors.toList());
    }

    private boolean hasMoreResults(Map<String, AttributeValue> startKey) {
        return startKey != null && !startKey.isEmpty();
    }

    protected boolean updateConditionFailed(Exception error) {
        return error instanceof ConditionalCheckFailedException;
    }


    private static NotFoundException handleFetchPublishingRequestByResoureError(SortableIdentifier resourceIdentifier) {
        return new NotFoundException(PUBLISHING_REQUEST_NOT_FOUND_FOR_RESOURCE + resourceIdentifier.toString());
    }

    private ApiGatewayException handleUpdatePublishingRequestFailure(Failure<UpdateItemResult> fail) {
        if (updateConditionFailed(fail.getException())) {
            return new no.unit.nva.publication.exception.BadRequestException(
                    UPDATE_PUBLISHING_REQUEST_STATUS_CONDITION_FAILURE_MESSAGE);
        }
        return new DynamoDBException(fail.getException());
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
    @JacocoGenerated
    protected Clock getClock() {
        return clock;
    }

}
