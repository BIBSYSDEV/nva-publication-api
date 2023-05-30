package no.unit.nva.publication.service;

import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import no.unit.nva.publication.TestDataSource;
import nva.commons.core.JacocoGenerated;
import org.junit.jupiter.api.AfterEach;

@JacocoGenerated
public class ResourcesLocalTest extends TestDataSource {

    public static final ScalarAttributeType STRING_TYPE = ScalarAttributeType.S;
    protected AmazonDynamoDB client;

    public ResourcesLocalTest() {
        super();
    }

    public void init() {
        client = DynamoDBEmbedded.create().amazonDynamoDB();
        CreateTableRequest request = createTableRequest();
        client.createTable(request);
    }

    public void initTwoTables(String resourceTable, String importCandidateTable) {
        client = DynamoDBEmbedded.create().amazonDynamoDB();
        client.createTable(createTableRequest(resourceTable));
        client.createTable(createTableRequest(importCandidateTable));
    }

    @AfterEach
    public void shutdown() {
        client.shutdown();
    }

    private CreateTableRequest createTableRequest() {
        return new CreateTableRequest()
                   .withTableName(RESOURCES_TABLE_NAME)
                   .withAttributeDefinitions(attributeDefinitions())
                   .withKeySchema(primaryKeySchema())
                   .withGlobalSecondaryIndexes(globalSecondaryIndexes())
                   .withBillingMode(BillingMode.PAY_PER_REQUEST);
    }

    private CreateTableRequest createTableRequest(String tableName) {
        return new CreateTableRequest()
                   .withTableName(tableName)
                   .withAttributeDefinitions(attributeDefinitions())
                   .withKeySchema(primaryKeySchema())
                   .withGlobalSecondaryIndexes(globalSecondaryIndexes())
                   .withBillingMode(BillingMode.PAY_PER_REQUEST);
    }

    private Collection<GlobalSecondaryIndex> globalSecondaryIndexes() {
        List<GlobalSecondaryIndex> indexes = new ArrayList<>();
        indexes.add(
            newGsi(BY_TYPE_CUSTOMER_STATUS_INDEX_NAME,
                   BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME,
                   BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME
            )
        );
        indexes.add(
            newGsi(BY_CUSTOMER_RESOURCE_INDEX_NAME,
                   BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME,
                   BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME)
        );
        indexes.add(
            newGsi(BY_TYPE_AND_IDENTIFIER_INDEX_NAME,
                   BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME,
                   BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME)
        );
        indexes.add(
            newGsi(RESOURCE_BY_CRISTIN_ID_INDEX_NAME,
                   RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME,
                   RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME)
        );
        return indexes;
    }

    private GlobalSecondaryIndex newGsi(String indexName, String partitionKeyName, String sortKeyName) {
        return new GlobalSecondaryIndex()
                   .withIndexName(indexName)
                   .withKeySchema(keySchema(partitionKeyName, sortKeyName))
                   .withProjection(new Projection().withProjectionType(ProjectionType.ALL));
    }

    private Collection<KeySchemaElement> primaryKeySchema() {
        return keySchema(PRIMARY_KEY_PARTITION_KEY_NAME, PRIMARY_KEY_SORT_KEY_NAME);
    }

    private Collection<KeySchemaElement> keySchema(String hashKey, String rangeKey) {
        List<KeySchemaElement> primaryKey = new ArrayList<>();
        primaryKey.add(newKeyElement(hashKey, KeyType.HASH));
        primaryKey.add(newKeyElement(rangeKey, KeyType.RANGE));
        return primaryKey;
    }

    private KeySchemaElement newKeyElement(String primaryKeySortKeyName, KeyType range) {
        return new KeySchemaElement().withAttributeName(primaryKeySortKeyName).withKeyType(range);
    }

    private AttributeDefinition[] attributeDefinitions() {
        List<AttributeDefinition> attributesList = new ArrayList<>();
        attributesList.add(newAttribute(PRIMARY_KEY_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(PRIMARY_KEY_SORT_KEY_NAME));
        attributesList.add(newAttribute(BY_TYPE_CUSTOMER_STATUS_INDEX_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(BY_TYPE_CUSTOMER_STATUS_INDEX_SORT_KEY_NAME));
        attributesList.add(newAttribute(BY_CUSTOMER_RESOURCE_INDEX_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(BY_CUSTOMER_RESOURCE_INDEX_SORT_KEY_NAME));
        attributesList.add(newAttribute(BY_TYPE_AND_IDENTIFIER_INDEX_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(BY_TYPE_AND_IDENTIFIER_INDEX_SORT_KEY_NAME));
        attributesList.add(newAttribute(RESOURCES_BY_CRISTIN_ID_INDEX_PARTITION_KEY_NAME));
        attributesList.add(newAttribute(RESOURCES_BY_CRISTIN_ID_INDEX_SORT_KEY_NAME));
        AttributeDefinition[] attributesArray = new AttributeDefinition[attributesList.size()];
        attributesList.toArray(attributesArray);
        return attributesArray;
    }

    private AttributeDefinition newAttribute(String keyName) {
        return new AttributeDefinition()
                   .withAttributeName(keyName)
                   .withAttributeType(STRING_TYPE);
    }
}
