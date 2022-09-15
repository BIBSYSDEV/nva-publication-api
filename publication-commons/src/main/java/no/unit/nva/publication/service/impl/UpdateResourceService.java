package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.model.business.PublishingRequestCase.assertThatPublicationHasMinimumMandatoryFields;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.primaryKeyEqualityConditionAttributeValues;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.Update;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.publication.exception.TransactionFailedException;
import no.unit.nva.publication.model.PublishPublicationStatusResponse;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.DoiRequestDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;

public class UpdateResourceService extends ServiceWithTransactions {
    
    public static final String DOI_REQUEST_FIELD_IN_DOI_REQUEST_DAO = "data";
    public static final String RESOURCE_STATUS_FIELD_IN_DOI_REQUEST = "resourceStatus";
    public static final String MODIFIED_DATE_FIELD_IN_DOI_REQUEST = "modifiedDate";
    
    public static final String PUBLISH_COMPLETED = "Publication is published.";
    public static final String PUBLISH_IN_PROGRESS = "Publication is being published. This may take a while.";
    public static final String RESOURCE_WITHOUT_MAIN_TITLE_ERROR = "Resource is missing main title: ";
    public static final String RESOURCE_LINK_FIELD = "link";
    
    private static final String PUBLISHED_DATE_FIELD_IN_RESOURCE = "publishedDate";
    //TODO: fix affiliation update when updating owner
    private static final URI AFFILIATION_UPDATE_NOT_UPDATE_YET = null;
    
    private final String tableName;
    private final AmazonDynamoDB client;
    private final Clock clockForTimestamps;
    private final ReadResourceService readResourceService;
    
    public UpdateResourceService(AmazonDynamoDB client,
                                 String tableName,
                                 Clock clockForTimestamps,
                                 ReadResourceService readResourceService) {
        super();
        this.tableName = tableName;
        this.client = client;
        this.clockForTimestamps = clockForTimestamps;
        this.readResourceService = readResourceService;
    }
    
    //TODO: here we allow all fields to be overwritten by the user.
    public Publication updatePublication(Publication publication) {
        var persistedPublication = fetchExistingPublication(publication);
        publication.setCreatedDate(persistedPublication.getCreatedDate());
        publication.setModifiedDate(clockForTimestamps.instant());
        var resource = Resource.fromPublication(publication);
        
        TransactWriteItem updateResourceTransactionItem = updateResource(resource);
        Optional<TransactWriteItem> updateDoiRequestTransactionItem = updateDoiRequest(resource);
        ArrayList<TransactWriteItem> transactionItems = new ArrayList<>();
        transactionItems.add(updateResourceTransactionItem);
        updateDoiRequestTransactionItem.ifPresent(transactionItems::add);
        
        TransactWriteItemsRequest request = new TransactWriteItemsRequest().withTransactItems(transactionItems);
        sendTransactionWriteRequest(request);
        
        return publication;
    }
    
    public void updateOwner(SortableIdentifier identifier, UserInstance oldOwner, UserInstance newOwner)
        throws NotFoundException {
        Resource existingResource = readResourceService.getResource(oldOwner, identifier);
        Resource newResource = updateResourceOwner(newOwner, existingResource);
        TransactWriteItem deleteAction = newDeleteTransactionItem(new ResourceDao(existingResource));
        TransactWriteItem insertionAction = newPutTransactionItem(new ResourceDao(newResource));
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(deleteAction, insertionAction);
        sendTransactionWriteRequest(request);
    }
    
    @Override
    protected AmazonDynamoDB getClient() {
        return client;
    }
    
    PublishPublicationStatusResponse publishResource(UserInstance userInstance,
                                                     SortableIdentifier resourceIdentifier)
        throws ApiGatewayException {
        List<Dao> daos = readResourceService
                             .fetchResourceAndDoiRequestFromTheByResourceIndex(userInstance, resourceIdentifier);
        var dao = extractResourceDao(daos);
        var resource = (Resource) dao.getData();
        if (resourceIsPublished(resource)) {
            return publishCompletedStatus();
        }
        assertThatPublicationHasMinimumMandatoryFields(resource.toPublication());
        setResourceStatusToPublished(daos, dao);
        return publishingInProgressStatus();
    }
    
    private Publication fetchExistingPublication(Publication publication) {
        return attempt(() -> readResourceService.getPublication(publication))
                   .orElseThrow(fail -> new TransactionFailedException(fail.getException()));
    }
    
    private boolean resourceIsPublished(Entity resource) {
        return PublicationStatus.PUBLISHED.equals(((Resource) resource).getStatus());
    }
    
    private Resource updateResourceOwner(UserInstance newOwner, Resource existingResource) {
        return existingResource
                   .copy()
                   .withPublisher(userOrganization(newOwner))
                   .withResourceOwner(Owner.fromResourceOwner(
                       new ResourceOwner(newOwner.getUserIdentifier(), AFFILIATION_UPDATE_NOT_UPDATE_YET)))
                   .withModifiedDate(clockForTimestamps.instant())
                   .build();
    }
    
    private Optional<TransactWriteItem> updateDoiRequest(Resource resource) {
        var queryObject = DoiRequest.createQueryObject(resource);
        var queryDao = queryObject.toDao();
        var existingDoiRequest = queryDao.fetchByResourceIdentifier(client)
                                     .map(Dao::getData)
                                     .map(DoiRequest.class::cast);
    
        return existingDoiRequest.map(doiRequest -> doiRequest.update(resource))
                   .map(DoiRequestDao::new).map(DoiRequestDao::toDynamoFormat)
                   .map(dynamoEntry -> new Put().withTableName(tableName).withItem(dynamoEntry))
                   .map(put -> new TransactWriteItem().withPut(put));
    }
    
    private TransactWriteItem updateResource(Resource resourceUpdate) {
        
        ResourceDao resourceDao = new ResourceDao(resourceUpdate);
        
        Map<String, AttributeValue> primaryKeyConditionAttributeValues =
            primaryKeyEqualityConditionAttributeValues(resourceDao);
    
        Put put = new Put()
                      .withItem(resourceDao.toDynamoFormat())
                      .withTableName(tableName)
                      .withConditionExpression(PRIMARY_KEY_EQUALITY_CHECK_EXPRESSION)
                      .withExpressionAttributeNames(PRIMARY_KEY_EQUALITY_CONDITION_ATTRIBUTE_NAMES)
                      .withExpressionAttributeValues(primaryKeyConditionAttributeValues);
        
        return new TransactWriteItem().withPut(put);
    }
    
    private void setResourceStatusToPublished(List<Dao> daos, ResourceDao resourceDao) {
        List<TransactWriteItem> transactionItems = createUpdateTransactionItems(daos, resourceDao);
        
        TransactWriteItemsRequest transactWriteItemsRequest = newTransactWriteItemsRequest(transactionItems);
        sendTransactionWriteRequest(transactWriteItemsRequest);
    }
    
    private List<TransactWriteItem> createUpdateTransactionItems(List<Dao> daos, ResourceDao resourceDao) {
        String nowString = nowAsString();
        List<TransactWriteItem> transactionItems = new ArrayList<>();
        transactionItems.add(publishUpdateRequest(resourceDao, nowString));
        Optional<DoiRequestDao> doiRequestDao = extractDoiRequest(daos);
        doiRequestDao.ifPresent(dao -> transactionItems.add(updateDoiRequestResourceStatus(dao, nowString)));
        return transactionItems;
    }
    
    private TransactWriteItem publishUpdateRequest(ResourceDao dao, String nowString) {
    
        var resource = (Resource) dao.getData();
        resource.setStatus(PublicationStatus.PUBLISHED);
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
    
    private PublishPublicationStatusResponse publishingInProgressStatus() {
        return new PublishPublicationStatusResponse(PUBLISH_IN_PROGRESS, HttpURLConnection.HTTP_ACCEPTED);
    }
    
    private PublishPublicationStatusResponse publishCompletedStatus() {
        return new PublishPublicationStatusResponse(PUBLISH_COMPLETED, HttpURLConnection.HTTP_NO_CONTENT);
    }
}
