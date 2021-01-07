package no.unit.nva.publication.service.impl;

import static nva.commons.utils.JsonUtils.objectMapper;
import static nva.commons.utils.attempt.Try.attempt;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemUtils;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import java.util.Map;
import no.unit.nva.publication.storage.model.DatabaseConstants;
import no.unit.nva.publication.storage.model.Resource;
import no.unit.nva.publication.storage.model.ResourceDao;

public class ResourceService {

    private final String tableName;
    //    private final String byTypeCustomerStatusIndexName;

    private final AmazonDynamoDB client;

    public ResourceService(AmazonDynamoDB client) {
        tableName = DatabaseConstants.RESOURCES_TABLE_NAME;
        //        byTypeCustomerStatusIndexName = DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME;
        this.client = client;
    }

    public ResourceService() {
        this(AmazonDynamoDBClientBuilder.defaultClient());
    }

    public void createResource(Resource resource) {
        client.putItem(new PutItemRequest()
            .withTableName(tableName)
            .withItem(toDynamoFormat(new ResourceDao(resource))));
    }

    public Resource getResource(Resource resource) {
        Map<String, AttributeValue> primaryKey = new ResourceDao(resource).primaryKey();
        GetItemResult getResult = getResourceByPrimaryKey(primaryKey);
        ResourceDao fetchedDao = parseResultToDao(getResult);
        return fetchedDao.getResource();
    }

    private ResourceDao parseResultToDao(GetItemResult getResult) {
        Item item = ItemUtils.toItem(getResult.getItem());
        return attempt(() -> objectMapper.readValue(item.toJSON(), ResourceDao.class)).orElseThrow();
    }

    private GetItemResult getResourceByPrimaryKey(Map<String, AttributeValue> primaryKey) {
        return client.getItem(new GetItemRequest()
            .withTableName(tableName)
            .withKey(primaryKey));
    }

    private <T> Map<String, AttributeValue> toDynamoFormat(T element) {
        Item item = attempt(() -> Item.fromJSON(objectMapper.writeValueAsString(element))).orElseThrow();
        return ItemUtils.toAttributeValues(item);
    }
}
