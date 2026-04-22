package no.unit.nva.publication.adapter;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_1_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_1_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.GSI_1_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCE_BY_CRISTIN_ID_INDEX_NAME;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.BillingMode;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProjectionType;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import java.util.List;

public final class LocalDynamoDb {

    private LocalDynamoDb() {
    }

    public static AmazonDynamoDB startAndCreateTable(String tableName) {
        var client = DynamoDBEmbedded.create().amazonDynamoDB();
        client.createTable(buildCreateTableRequest(tableName));
        return client;
    }

    private static CreateTableRequest buildCreateTableRequest(String tableName) {
        return new CreateTableRequest()
            .withTableName(tableName)
            .withAttributeDefinitions(attributeDefinitions())
            .withKeySchema(primaryKeySchema())
            .withGlobalSecondaryIndexes(globalSecondaryIndexes())
            .withBillingMode(BillingMode.PAY_PER_REQUEST);
    }

    private static List<GlobalSecondaryIndex> globalSecondaryIndexes() {
        return List.of(
            gsi(GSI_1_INDEX_NAME,
                GSI_1_PARTITION_KEY_NAME,
                GSI_1_SORT_KEY_NAME),
            gsi(BY_CUSTOMER_RESOURCE_INDEX_NAME,
                BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME,
                BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME),
            gsi(BY_TYPE_AND_IDENTIFIER_INDEX_NAME,
                BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME,
                BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME),
            gsi(RESOURCE_BY_CRISTIN_ID_INDEX_NAME,
                RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME,
                RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME));
    }

    private static GlobalSecondaryIndex gsi(String indexName, String hashKey, String rangeKey) {
        return new GlobalSecondaryIndex()
            .withIndexName(indexName)
            .withKeySchema(keySchema(hashKey, rangeKey))
            .withProjection(new Projection().withProjectionType(ProjectionType.ALL));
    }

    private static List<KeySchemaElement> primaryKeySchema() {
        return keySchema(PRIMARY_KEY_PARTITION_KEY_NAME, PRIMARY_KEY_SORT_KEY_NAME);
    }

    private static List<KeySchemaElement> keySchema(String hashKey, String rangeKey) {
        return List.of(
            new KeySchemaElement().withAttributeName(hashKey).withKeyType(KeyType.HASH),
            new KeySchemaElement().withAttributeName(rangeKey).withKeyType(KeyType.RANGE));
    }

    private static List<AttributeDefinition> attributeDefinitions() {
        return List.of(
            attribute(PRIMARY_KEY_PARTITION_KEY_NAME),
            attribute(PRIMARY_KEY_SORT_KEY_NAME),
            attribute(GSI_1_PARTITION_KEY_NAME),
            attribute(GSI_1_SORT_KEY_NAME),
            attribute(BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME),
            attribute(BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME),
            attribute(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME),
            attribute(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME),
            attribute(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME),
            attribute(RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME));
    }

    private static AttributeDefinition attribute(String name) {
        return new AttributeDefinition().withAttributeName(name).withAttributeType(ScalarAttributeType.S);
    }
}
