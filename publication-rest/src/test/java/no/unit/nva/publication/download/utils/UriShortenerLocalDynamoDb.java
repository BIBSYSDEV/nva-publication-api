package no.unit.nva.publication.download.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TimeToLiveSpecification;
import software.amazon.awssdk.services.dynamodb.model.UpdateTimeToLiveRequest;
import software.amazon.dynamodb.services.local.embedded.DynamoDBEmbedded;


public class UriShortenerLocalDynamoDb {

    public static final String URI_MAP_PRIMARY_PARTITION_KEY = "shortenedUri";

    public static final ScalarAttributeType STRING_TYPE = ScalarAttributeType.S;
    protected DynamoDbClient client;
    private String uriMapTableName;

    public void setUriMapTableName(String uriMapTableName) {
        this.uriMapTableName = uriMapTableName;
    }

    public void init(String uriMapTableName) {
        setUriMapTableName(uriMapTableName);
        client = DynamoDBEmbedded.create(null, true).dynamoDbClient();
        var request = createTableRequest();
        client.createTable(request);
        updateWithTimeToLive();
    }

    private void updateWithTimeToLive() {
        var timeToLiveSpecification = TimeToLiveSpecification.builder()
                                          .attributeName("expiresDate")
                                          .enabled(true)
                                          .build();

        var updateTimeToliveRequest = UpdateTimeToLiveRequest.builder()
                                          .tableName(uriMapTableName)
                                          .timeToLiveSpecification(timeToLiveSpecification)
                                          .build();

        client.updateTimeToLive(updateTimeToliveRequest);
    }

    @AfterEach
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    private CreateTableRequest createTableRequest() {
        return CreateTableRequest.builder()
                   .tableName(uriMapTableName)
                   .attributeDefinitions(attributeDefinitions())
                   .keySchema(primaryKeySchema())
                   .billingMode(BillingMode.PAY_PER_REQUEST)
                   .build();
    }

    private List<AttributeDefinition> attributeDefinitions() {
        var attributesList = new ArrayList<AttributeDefinition>();
        attributesList.add(newAttribute(URI_MAP_PRIMARY_PARTITION_KEY));
        return attributesList;
    }

    private Collection<KeySchemaElement> primaryKeySchema() {
        return keySchema(URI_MAP_PRIMARY_PARTITION_KEY);
    }

    private Collection<KeySchemaElement> keySchema(String hashKey) {
        var primaryKey = new ArrayList<KeySchemaElement>();
        primaryKey.add(newKeyElement(hashKey, KeyType.HASH));
        return primaryKey;
    }

    private KeySchemaElement newKeyElement(String primaryKeySortKeyName, KeyType range) {
        return KeySchemaElement.builder()
                   .attributeName(primaryKeySortKeyName)
                   .keyType(range)
                   .build();
    }

    private AttributeDefinition newAttribute(String keyName) {
        return AttributeDefinition.builder()
                   .attributeName(keyName)
                   .attributeType(STRING_TYPE)
                   .build();
    }
}
