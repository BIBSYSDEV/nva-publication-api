package no.unit.nva.publication.service.impl;

import static java.util.Objects.isNull;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.KEY_EXISTS_CONDITION;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PARTITION_KEY_NAME_PLACEHOLDER;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.PRIMARY_KEY_PLACEHOLDERS_AND_ATTRIBUTE_NAMES_MAPPING;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.RESOURCE_NOT_FOUND_MESSAGE;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.SORT_KEY_NAME_PLACEHOLDER;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.newPutTransactionItem;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.newTransactWriteItemsRequest;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.parseResult;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.toDynamoFormat;
import static no.unit.nva.publication.service.impl.ResourceServiceUtils.userOrganization;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
import static nva.commons.utils.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Delete;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.Put;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItem;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import java.time.Clock;
import java.util.Map;
import no.unit.nva.publication.identifiers.SortableIdentifier;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.daos.IdentifierEntry;
import no.unit.nva.publication.storage.model.daos.ResourceDao;
import no.unit.nva.publication.storage.model.daos.WithPrimaryKey;
import nva.commons.exceptions.commonexceptions.ConflictException;
import nva.commons.exceptions.commonexceptions.NotFoundException;
import nva.commons.utils.attempt.Failure;

public class ResourceService {

    private static final String PARTITION_KEY_VALUE_PLACEHOLDER = ":partitionKey";
    private static final String SORT_KEY_VALUE_PLACEHOLDER = ":sortKey";
    private final String tableName;
    private final AmazonDynamoDB client;
    private final Clock clockForTimestamps;

    public ResourceService(AmazonDynamoDB client, Clock clock) {
        tableName = RESOURCES_TABLE_NAME;
        this.client = client;
        this.clockForTimestamps = clock;
    }

    public ResourceService() {
        this(AmazonDynamoDBClientBuilder.defaultClient(), Clock.systemDefaultZone());
    }

    public void createResource(Resource resource) throws ConflictException {
        resource.setCreatedDate(clockForTimestamps.instant());
        TransactWriteItem[] transactionItems = transactionItemsForNewResourceInsertion(resource);
        TransactWriteItemsRequest putRequest = newTransactWriteItemsRequest(transactionItems);
        sendRequest(putRequest);
    }

    public void updateResource(Resource resourceUpdate)  {
        ResourceDao resourceDao = new ResourceDao(resourceUpdate);
        String conditionExpression = primaryKeyHasNotChangedCondition();

        Map<String, AttributeValue> valuesMap = primaryKeyAttributeValuesMap(resourceDao);

        PutItemRequest putItemRequest = new PutItemRequest()
            .withItem(toDynamoFormat(resourceDao))
            .withTableName(tableName)
            .withConditionExpression(conditionExpression)
            .withExpressionAttributeNames(PRIMARY_KEY_PLACEHOLDERS_AND_ATTRIBUTE_NAMES_MAPPING)
            .withExpressionAttributeValues(valuesMap);

        client.putItem(putItemRequest);
    }

    public Resource getResource(UserInstance userInstance, SortableIdentifier resourceIdentifier)
        throws NotFoundException {
        return getResource(createQueryObject(userInstance, resourceIdentifier.toString()));
    }

    public Resource getResource(UserInstance userInstance, String resourceIdentifier) throws NotFoundException {
        return getResource(createQueryObject(userInstance, resourceIdentifier));
    }

    public Resource getResource(Resource resource) throws NotFoundException {
        Map<String, AttributeValue> primaryKey = new ResourceDao(resource).primaryKey();
        GetItemResult getResult = getResourceByPrimaryKey(primaryKey);
        ResourceDao fetchedDao = parseResult(getResult);
        return fetchedDao.getResource();
    }

    public void updateOwner(String identifier, UserInstance oldOwner, UserInstance newOwner) throws NotFoundException {
        Resource existingResource = getResource(oldOwner, identifier);
        Resource newResource = updateResourceOwner(newOwner, existingResource);
        TransactWriteItem deleteAction = newDeleteTransactionItem(existingResource);
        TransactWriteItem insertionAction = newPutTransactionItem(createNewTransactionPutDataEntry(newResource));
        TransactWriteItemsRequest request = newTransactWriteItemsRequest(deleteAction, insertionAction);
        client.transactWriteItems(request);
    }

    private Map<String, AttributeValue> primaryKeyAttributeValuesMap(ResourceDao resourceDao) {
        return Map.of(PARTITION_KEY_VALUE_PLACEHOLDER,
            new AttributeValue(resourceDao.getPrimaryKeyPartitionKey()),
            SORT_KEY_VALUE_PLACEHOLDER, new AttributeValue(resourceDao.getPrimaryKeySortKey()));
    }

    private String primaryKeyHasNotChangedCondition() {
        return String.format("%s = %s AND %s = %s",
            PARTITION_KEY_NAME_PLACEHOLDER, PARTITION_KEY_VALUE_PLACEHOLDER, SORT_KEY_NAME_PLACEHOLDER,
            SORT_KEY_VALUE_PLACEHOLDER);
    }

    private void sendRequest(TransactWriteItemsRequest putRequest) throws ConflictException {
        attempt(() -> client.transactWriteItems(putRequest)).orElseThrow(this::handleTransactionFailure);
    }

    private Resource updateResourceOwner(UserInstance newOwner, Resource existingResource) {
        return existingResource.copy()
            .withPublisher(userOrganization(newOwner))
            .withOwner(newOwner.getUserId())
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
        return Resource.emptyResource(userInstance.getUserId(), userInstance.getOrganizationUri(), resourceIdentifier);
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
