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

import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

public final class ResourceTable {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTable.class);
    private static final String SHORTENED_URI_KEY = "shortenedUri";

    private ResourceTable() {
    }

    /**
     * The shortened download URLs live in their own table, keyed only by the shortened URI. See
     * DownloadUrlShorteningTable in template.yaml.
     */
    public static void createShortenedUriTableIfMissing(DynamoDbClient client, String tableName) {
        var request = CreateTableRequest.builder()
            .tableName(tableName)
            .attributeDefinitions(attribute(SHORTENED_URI_KEY))
            .keySchema(KeySchemaElement.builder()
                           .attributeName(SHORTENED_URI_KEY)
                           .keyType(KeyType.HASH)
                           .build())
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build();
        try {
            client.createTable(request);
            logger.info("Created table {}", tableName);
        } catch (ResourceInUseException e) {
            logger.info("Table {} already exists", tableName);
        }
    }

    public static void createIfMissing(DynamoDbClient client, String tableName) {
        try {
            client.createTable(buildCreateTableRequest(tableName));
            logger.info("Created table {} with {} global secondary indexes", tableName,
                        globalSecondaryIndexes().size());
        } catch (ResourceInUseException e) {
            logger.info("Table {} already exists", tableName);
        }
    }

    private static CreateTableRequest buildCreateTableRequest(String tableName) {
        return CreateTableRequest.builder()
            .tableName(tableName)
            .attributeDefinitions(attributeDefinitions())
            .keySchema(primaryKeySchema())
            .globalSecondaryIndexes(globalSecondaryIndexes())
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build();
    }

    private static Collection<GlobalSecondaryIndex> globalSecondaryIndexes() {
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
        return GlobalSecondaryIndex.builder()
            .indexName(indexName)
            .keySchema(keySchema(hashKey, rangeKey))
            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
            .build();
    }

    private static Collection<KeySchemaElement> primaryKeySchema() {
        return keySchema(PRIMARY_KEY_PARTITION_KEY_NAME, PRIMARY_KEY_SORT_KEY_NAME);
    }

    private static Collection<KeySchemaElement> keySchema(String hashKey, String rangeKey) {
        return List.of(
            KeySchemaElement.builder().attributeName(hashKey).keyType(KeyType.HASH).build(),
            KeySchemaElement.builder().attributeName(rangeKey).keyType(KeyType.RANGE).build());
    }

    private static Collection<AttributeDefinition> attributeDefinitions() {
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
        return AttributeDefinition.builder()
            .attributeName(name)
            .attributeType(ScalarAttributeType.S)
            .build();
    }
}
