package no.unit.nva.publication.download.utils;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveSpecification;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;


public class UriShortenerLocalDynamoDb {

    public static final String URI_MAP_PRIMARY_PARTITION_KEY = "shortenedUri";


    public static final ScalarAttributeType STRING_TYPE = ScalarAttributeType.S;
    protected AmazonDynamoDB client;
    private String uriMapTableName;

    public void setUriMapTableName(String uriMapTableName) {
        this.uriMapTableName = uriMapTableName;
    }

    public void init(String uriMapTableName) {
        setUriMapTableName(uriMapTableName);
        client = DynamoDBEmbedded.create().amazonDynamoDB();
        CreateTableRequest request = createTableRequest();
        client.createTable(request);
        updateWithTimeToLive();
    }

    private void updateWithTimeToLive() {
        var timeToLiveSpecification = new TimeToLiveSpecification();
        timeToLiveSpecification.withAttributeName("expiresDate");
        timeToLiveSpecification.withEnabled(true);

        var updateTimeToliveRequest = new UpdateTimeToLiveRequest();
        updateTimeToliveRequest.setTableName(uriMapTableName);
        updateTimeToliveRequest.setTimeToLiveSpecification(timeToLiveSpecification);

        client.updateTimeToLive(updateTimeToliveRequest);
    }

    @AfterEach
    public void shutdown() {
        client.shutdown();
    }

    private CreateTableRequest createTableRequest() {
        return new CreateTableRequest()
                   .withTableName(uriMapTableName)
                   .withAttributeDefinitions(attributeDefinitions())
                   .withKeySchema(primaryKeySchema())
                   .withBillingMode(BillingMode.PAY_PER_REQUEST);
    }

    private AttributeDefinition[] attributeDefinitions() {
        List<AttributeDefinition> attributesList = new ArrayList<>();
        attributesList.add(newAttribute(URI_MAP_PRIMARY_PARTITION_KEY));
        AttributeDefinition[] attributesArray = new AttributeDefinition[attributesList.size()];
        attributesList.toArray(attributesArray);
        return attributesArray;
    }

    private Collection<KeySchemaElement> primaryKeySchema() {
        return keySchema(URI_MAP_PRIMARY_PARTITION_KEY);
    }

    private Collection<KeySchemaElement> keySchema(String hashKey) {
        List<KeySchemaElement> primaryKey = new ArrayList<>();
        primaryKey.add(newKeyElement(hashKey, KeyType.HASH));
        return primaryKey;
    }

    private KeySchemaElement newKeyElement(String primaryKeySortKeyName, KeyType range) {
        return new KeySchemaElement().withAttributeName(primaryKeySortKeyName).withKeyType(range);
    }

    private AttributeDefinition newAttribute(String keyName) {
        return new AttributeDefinition()
                   .withAttributeName(keyName)
                   .withAttributeType(STRING_TYPE);
    }
}
