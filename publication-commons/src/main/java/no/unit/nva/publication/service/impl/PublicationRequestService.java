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
import no.unit.nva.publication.storage.model.PublicationRequest;
import no.unit.nva.publication.storage.model.PublicationRequestStatus;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.PublicationRequestDao;
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

public class PublicationRequestService extends ServiceWithTransactions {

    public static final String PUBLICATION_REQUEST_NOT_FOUND_FOR_RESOURCE = "Could not find a PublicationRequest for Resource: ";
    public static final String ALREADY_PUBLISHED_OR_DELETED_AND_CANNOT_BE_PUBLISHED = "Publication is already published or deleted and cannot be published";
    public static final String UPDATE_PUBLICATION_REQUEST_STATUS_CONDITION_FAILURE_MESSAGE =
            "Could not update publishPublicationRequest status.";


    private static final int DEFAULT_QUERY_RESULT_SIZE = 10_000;

    private final AmazonDynamoDB client;
    private final Clock clock;
    private final String tableName;
    private final Supplier<SortableIdentifier> identifierProvider;
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;



    public PublicationRequestService(AmazonDynamoDB client,Clock clock) {
        this(client,
                clock,
                DEFAULT_IDENTIFIER_PROVIDER);
    }

    protected PublicationRequestService(AmazonDynamoDB client,
                                Clock clock,
                                Supplier<SortableIdentifier> identifierProvider) {
        super();
        this.client = client;
        this.clock = clock;
        this.tableName = RESOURCES_TABLE_NAME;
        this.identifierProvider = identifierProvider;
    }

    public void createPublicationRequest(Publication publication) throws BadRequestException, TransactionFailedException {
        verifyPublicationIsPublishable(publication);
        var publicationRequest = createNewPublicationRequestEntry(publication);
        var request = createInsertionTransactionRequest(publicationRequest);
        sendTransactionWriteRequest(request);
    }

    public PublicationRequest getPublicationRequest(UserInstance resourceOwner,
                                                    SortableIdentifier resourceIdentifier)
            throws NotFoundException {
        var queryResult = getQueryResult(resourceOwner, resourceIdentifier);

        var item = parseQueryResultExpectingSingleItem(queryResult)
                .orElseThrow(fail -> handleFetchPublicationRequestByResoureError(resourceIdentifier));
        return parseAttributeValuesMap(item, PublicationRequestDao.class).getData();
    }

    private QueryResult getQueryResult(UserInstance resourceOwner, SortableIdentifier resourceIdentifier) {
        var queryObject = PublicationRequestDao.queryByCustomerAndResourceIdentifier(resourceOwner,
                resourceIdentifier);
        var queryRequest = new QueryRequest()
                .withTableName(tableName)
                .withIndexName(BY_CUSTOMER_RESOURCE_INDEX_NAME)
                .withKeyConditions(queryObject.byResource(PublicationRequestDao.joinByResourceContainedOrderedType()));
        return client.query(queryRequest);
    }

    public List<PublicationRequest> listPublicationRequestsForUser(UserInstance userInstance) {
        var query = listPublicationRequestForUserQuery(userInstance, DEFAULT_QUERY_RESULT_SIZE);
        return performQueryWithPotentiallyManyResults(query);
    }

    public void updatePublicationRequest(UserInstance userInstance,
                                         SortableIdentifier resourceIdentifier,
                                         PublicationRequestStatus status) throws ApiGatewayException {

        var updateItemRequest = createRequestForUpdatingPublicationRequest(userInstance, resourceIdentifier, status);

        var item = attempt(() -> client.updateItem(updateItemRequest))
                .orElseThrow(this::handleUpdatePublicationRequestFailure);
        parseAttributeValuesMap(item.getAttributes(), PublicationRequestDao.class);
    }

    private void verifyPublicationIsPublishable(Publication publication) throws BadRequestException {
        if (publication.getStatus() == PublicationStatus.PUBLISHED
                || publication.getStatus() == PublicationStatus.DRAFT_FOR_DELETION) {
            throw new BadRequestException(ALREADY_PUBLISHED_OR_DELETED_AND_CANNOT_BE_PUBLISHED);
        }
    }

    private PublicationRequest createNewPublicationRequestEntry(Publication publication) {
        var resource = Resource.fromPublication(publication);
        return PublicationRequest.newPublicationRequestResource(identifierProvider.get(), resource, clock.instant());
    }

    private TransactWriteItemsRequest createInsertionTransactionRequest(PublicationRequest publicationRequest) {
        var publicationRequestEntry = createPublicationRequestInsertionEntry(publicationRequest);
        var identifierEntry = createUniqueIdentifierEntry(publicationRequest);
        var uniquePublicationRequestEntry = createUniquePublicationRequestEntry(publicationRequest);

        return new TransactWriteItemsRequest()
                .withTransactItems(
                        identifierEntry,
                        uniquePublicationRequestEntry,
                        publicationRequestEntry);
    }

    private TransactWriteItem createPublicationRequestInsertionEntry(PublicationRequest publicationRequest) {
        return newPutTransactionItem(new PublicationRequestDao(publicationRequest));
    }

    private TransactWriteItem createUniquePublicationRequestEntry(PublicationRequest publicationRequest) {
        var uniquePublicationRequestEntry = new UniquePublicationRequestEntry(
                publicationRequest.getResourceIdentifier().toString());
        return newPutTransactionItem(uniquePublicationRequestEntry);
    }

    private TransactWriteItem createUniqueIdentifierEntry(PublicationRequest publicationRequest) {
        var identifierEntry = new IdentifierEntry(publicationRequest.getIdentifier().toString());
        return newPutTransactionItem(identifierEntry);
    }

    private QueryRequest listPublicationRequestForUserQuery(UserInstance userInstance, int maxResultSize) {
        var queryExpression = "#PK= :PK";
        var filterExpression = "#data.#status = :pendingStatus OR #data.#status = :rejectedStatus";

        var expressionAttributeNames =
                Map.of(
                        "#PK", DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME,
                        "#data", PublicationRequestDao.CONTAINED_DATA_FIELD_NAME,
                        "#status", PublicationRequest.STATUS_FIELD);

        var primaryKeyPartitionKeyValue = publicationRequestPrimaryKeyPartionKeyValue(userInstance);

        var expressionAttributeValues = Map.of(
                ":PK", new AttributeValue(primaryKeyPartitionKeyValue),
                ":pendingStatus", new AttributeValue(PublicationRequestStatus.PENDING.toString()),
                ":rejectedStatus", new AttributeValue(PublicationRequestStatus.REJECTED.toString())
        );

        return new QueryRequest()
                .withTableName(tableName)
                .withKeyConditionExpression(queryExpression)
                .withFilterExpression(filterExpression)
                .withExpressionAttributeNames(expressionAttributeNames)
                .withExpressionAttributeValues(expressionAttributeValues)
                .withLimit(maxResultSize);
    }

    protected UpdateItemRequest createRequestForUpdatingPublicationRequest(UserInstance userInstance,
                                                                 SortableIdentifier resourceIdentifier,
                                                                 PublicationRequestStatus status) throws NotFoundException {
        var now = nowAsString();

        var dao = createUpdatedPublicationRequestDao(userInstance, resourceIdentifier, status);
        var updateExpression = "SET"
                + "#data.#status = :status, "
                + "#data.#modifiedDate = :modifiedDate,"
                + "#PK1 = :PK1 ,"
                + "#SK1 = :SK1 ,"
                + "#PK2 = :PK2 ,"
                + "#SK2 = :SK2 ";

        var conditionExpression = "#data.#resourceStatus <> :publishedStatus";

        var expressionAttributeNames = Map.of(
                "#data", PublicationRequestDao.CONTAINED_DATA_FIELD_NAME,
                "#status", PublicationRequest.STATUS_FIELD,
                "#modifiedDate", PublicationRequest.MODIFIED_DATE_FIELD,
                "#resourceStatus", PublicationRequest.RESOURCE_STATUS_FIELD,
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

    private PublicationRequestDao createUpdatedPublicationRequestDao(UserInstance userInstance, SortableIdentifier resourceIdentifier,
                                                     PublicationRequestStatus status) throws NotFoundException {
        var publicationRequest = getPublicationRequest(userInstance, resourceIdentifier);
        var existingStatus = publicationRequest.getStatus();
        publicationRequest.setStatus(existingStatus.changeStatus(status));
        return new PublicationRequestDao(publicationRequest);
    }

    private String publicationRequestPrimaryKeyPartionKeyValue(UserInstance userInstance) {
        var queryObject = PublicationRequestDao.queryObject(userInstance.getOrganizationUri(),
                userInstance.getUserIdentifier());
        return queryObject.getPrimaryKeyPartitionKey();
    }

    private static Try<Map<String, AttributeValue>> parseQueryResultExpectingSingleItem(QueryResult queryResult) {
        return attempt(() -> queryResult.getItems()
                .stream()
                .collect(SingletonCollector.collect()));
    }

    private List<PublicationRequest> performQueryWithPotentiallyManyResults(QueryRequest query) {
        Map<String, AttributeValue> startKey = null;
        var result = new ArrayList<PublicationRequest>();
        do {
            query.withExclusiveStartKey(startKey);
            var queryResult = client.query(query);
            var lastItems = parseListingPublicationRequestsQueryResult(queryResult);
            result.addAll(lastItems);
            startKey = queryResult.getLastEvaluatedKey();
        } while (hasMoreResults(startKey));
        return result;
    }

    private List<PublicationRequest> parseListingPublicationRequestsQueryResult(QueryResult result) {
        return result.getItems()
                .stream()
                .map(map -> parseAttributeValuesMap(map, PublicationRequestDao.class))
                .map(PublicationRequestDao::getData)
                .collect(Collectors.toList());
    }

    private boolean hasMoreResults(Map<String, AttributeValue> startKey) {
        return startKey != null && !startKey.isEmpty();
    }

    protected boolean updateConditionFailed(Exception error) {
        return error instanceof ConditionalCheckFailedException;
    }


    private static NotFoundException handleFetchPublicationRequestByResoureError(SortableIdentifier resourceIdentifier) {
        return new NotFoundException(PUBLICATION_REQUEST_NOT_FOUND_FOR_RESOURCE + resourceIdentifier.toString());
    }

    private ApiGatewayException handleUpdatePublicationRequestFailure(Failure<UpdateItemResult> fail) {
        if (updateConditionFailed(fail.getException())) {
            return new no.unit.nva.publication.exception.BadRequestException(
                    UPDATE_PUBLICATION_REQUEST_STATUS_CONDITION_FAILURE_MESSAGE);
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
