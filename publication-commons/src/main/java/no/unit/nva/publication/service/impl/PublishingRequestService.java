package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static no.unit.nva.publication.storage.model.daos.Dao.CONTAINED_DATA_FIELD_NAME;
import static no.unit.nva.publication.storage.model.daos.DynamoEntry.parseAttributeValuesMap;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ReturnValue;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.storage.model.DataEntry;
import no.unit.nva.publication.storage.model.PublishingRequestCase;
import no.unit.nva.publication.storage.model.UserInstance;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.PublishingRequestDao;
import no.unit.nva.publication.storage.model.daos.UniquePublishingRequestEntry;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class PublishingRequestService extends ServiceWithTransactions {

    public static final String PUBLISHING_REQUEST_NOT_FOUND_FOR_RESOURCE =
        "Could not find a Publishing Request for Resource: ";
    public static final String ALREADY_PUBLISHED_ERROR =
        "Publication is already published.";
    public static final String MARKED_FOR_DELETION_ERROR =
        "Publication is marked for deletion and cannot be published.";
    private static final Supplier<SortableIdentifier> DEFAULT_IDENTIFIER_PROVIDER = SortableIdentifier::next;
    private final AmazonDynamoDB client;
    private final Clock clock;
    private final String tableName;
    private final Supplier<SortableIdentifier> identifierProvider;
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

    public PublishingRequestCase createPublishingRequest(PublishingRequestCase publishingRequest)
        throws ApiGatewayException {
        var associatePublication = fetchPublication(publishingRequest);
        return fromPublication(associatePublication);
    }

    public PublishingRequestCase getPublishingRequest(PublishingRequestCase queryObject)
        throws NotFoundException {

        var queryResult = getFromDatabase(queryObject);
        return attempt(queryResult::getItem)
            .map(item -> parseAttributeValuesMap(item, PublishingRequestDao.class))
            .map(PublishingRequestDao::getData)
            .toOptional()
            .orElseThrow(() -> handleFetchPublishingRequestByResourceError(queryObject.getIdentifier()));
    }

    public void updatePublishingRequest(PublishingRequestCase requestUpdate) throws ApiGatewayException {

        var updateItemRequest = createUpdateDatabaseItemRequest(requestUpdate);

        var item = client.updateItem(updateItemRequest);
        parseAttributeValuesMap(item.getAttributes(), PublishingRequestDao.class);
    }

    protected UpdateItemRequest createUpdateDatabaseItemRequest(PublishingRequestCase requestUpdate)
        throws NotFoundException {
        var now = nowAsString();

        var dao = createUpdatedPublishingRequestDao(requestUpdate);
        var updateExpression = "SET"
                               + "#data.#status = :status, "
                               + "#data.#modifiedDate = :modifiedDate,"
                               + "#data.#rowVersion = :rowVersion,"
                               + "#PK1 = :PK1 ,"
                               + "#SK1 = :SK1 ,"
                               + "#PK2 = :PK2 ,"
                               + "#SK2 = :SK2 ";

        var conditionExpression = "#data.#resourceStatus <> :publishedStatus";

        var expressionAttributeNames = Map.of(
            "#data", CONTAINED_DATA_FIELD_NAME,
            "#status", PublishingRequestCase.STATUS_FIELD,
            "#modifiedDate", PublishingRequestCase.MODIFIED_DATE_FIELD,
            "#resourceStatus", PublishingRequestCase.RESOURCE_STATUS_FIELD,
            "#rowVersion", DataEntry.ROW_VERSION,
            "#PK1", BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME,
            "#SK1", BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME,
            "#PK2", BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME,
            "#SK2", BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME

        );

        var expressionAttributeValues = Map.of(
            ":status", new AttributeValue(dao.getData().getStatus().name()),
            ":modifiedDate", new AttributeValue(now),
            ":publishedStatus", new AttributeValue(PublicationStatus.PUBLISHED.toString()),
            ":rowVersion", new AttributeValue(UUID.randomUUID().toString()),
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

    private static NotFoundException handleFetchPublishingRequestByResourceError(
        SortableIdentifier resourceIdentifier) {
        return new NotFoundException(PUBLISHING_REQUEST_NOT_FOUND_FOR_RESOURCE + resourceIdentifier.toString());
    }

    private Publication fetchPublication(PublishingRequestCase publishingRequest)
        throws ApiGatewayException {
        var userInstance = UserInstance.create(publishingRequest.getOwner(), publishingRequest.getCustomerId());
        return resourceService.getPublication(userInstance, publishingRequest.getResourceIdentifier());
    }

    private PublishingRequestCase fromPublication(Publication publication)
        throws ConflictException {
        verifyPublicationIsPublishable(publication);
        var publishingRequest = createNewPublishingRequestEntry(publication);
        var request = createInsertionTransactionRequest(publishingRequest);
        sendTransactionWriteRequest(request);
        return publishingRequest;
    }

    private GetItemResult getFromDatabase(PublishingRequestCase queryObject) {
        var queryDao = PublishingRequestDao.queryObject(queryObject);
        var getItemRequest = new GetItemRequest()
            .withTableName(tableName)
            .withKey(queryDao.primaryKey());
        return client.getItem(getItemRequest);
    }

    private void verifyPublicationIsPublishable(Publication publication) throws ConflictException {
        if (PublicationStatus.PUBLISHED == publication.getStatus()) {
            throw new ConflictException(ALREADY_PUBLISHED_ERROR);
        }
        if (PublicationStatus.DRAFT_FOR_DELETION == publication.getStatus()) {
            throw new ConflictException(MARKED_FOR_DELETION_ERROR);
        }
    }

    private PublishingRequestCase createNewPublishingRequestEntry(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        var entry = PublishingRequestCase.createOpeningCaseObject(userInstance, publication.getIdentifier());
        entry.setCreatedDate(clock.instant());
        entry.setIdentifier(identifierProvider.get());
        entry.setRowVersion(UUID.randomUUID().toString());
        return entry;
    }

    private TransactWriteItemsRequest createInsertionTransactionRequest(PublishingRequestCase publishingRequest) {
        var publicationRequestEntry = createPublishingRequestInsertionEntry(publishingRequest);
        var identifierEntry = createUniqueIdentifierEntry(publishingRequest);
        var publishingRequestUniquenessEntry = createPublishingRequestUniquenessEntry(publishingRequest);
        return new TransactWriteItemsRequest()
            .withTransactItems(
                identifierEntry,
                publicationRequestEntry,
                publishingRequestUniquenessEntry);
    }

    private TransactWriteItem createPublishingRequestUniquenessEntry(PublishingRequestCase publishingRequest) {
        var publishingRequestUniquenessEntry = UniquePublishingRequestEntry.create(publishingRequest);
        return newPutTransactionItem(publishingRequestUniquenessEntry);
    }

    private TransactWriteItem createPublishingRequestInsertionEntry(PublishingRequestCase publicationRequest) {
        PublishingRequestDao dynamoEntry = new PublishingRequestDao(publicationRequest);
        return newPutTransactionItem(dynamoEntry);
    }

    private TransactWriteItem createUniqueIdentifierEntry(PublishingRequestCase publicationRequest) {
        var identifierEntry = new IdentifierEntry(publicationRequest.getIdentifier().toString());
        return newPutTransactionItem(identifierEntry);
    }

    private PublishingRequestDao createUpdatedPublishingRequestDao(PublishingRequestCase requestUpdate)
        throws NotFoundException {
        var publicationRequest = getPublishingRequest(requestUpdate);
        var existingStatus = publicationRequest.getStatus();
        publicationRequest.setStatus(existingStatus.changeStatus(requestUpdate.getStatus()));
        return new PublishingRequestDao(publicationRequest);
    }
}
