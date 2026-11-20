package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.dynamodb.services.local.embedded.DynamoDBEmbedded;

/** A local DynamoDB table with the same keys as the import candidates table. */
final class LocalImportCandidatesTable implements AutoCloseable {

  static final String DEFAULT_TABLE_NAME = "import-candidates";

  private final DynamoDbClient client;
  private final String tableName;

  LocalImportCandidatesTable() {
    this(DEFAULT_TABLE_NAME);
  }

  LocalImportCandidatesTable(String tableName) {
    this.tableName = tableName;
    this.client = DynamoDBEmbedded.create(null, true).dynamoDbClient();
    client.createTable(
        CreateTableRequest.builder()
            .tableName(tableName)
            .attributeDefinitions(
                stringAttribute(PRIMARY_KEY_PARTITION_KEY_NAME),
                stringAttribute(PRIMARY_KEY_SORT_KEY_NAME))
            .keySchema(
                keyElement(PRIMARY_KEY_PARTITION_KEY_NAME, KeyType.HASH),
                keyElement(PRIMARY_KEY_SORT_KEY_NAME, KeyType.RANGE))
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build());
  }

  DynamoDbClient client() {
    return client;
  }

  String tableName() {
    return tableName;
  }

  DynamoDbScopusMappingRepository repository() {
    return new DynamoDbScopusMappingRepository(client, tableName);
  }

  void putItem(Map<String, AttributeValue> item) {
    client.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
  }

  @Override
  public void close() {
    client.close();
  }

  private static AttributeDefinition stringAttribute(String attributeName) {
    return AttributeDefinition.builder()
        .attributeName(attributeName)
        .attributeType(ScalarAttributeType.S)
        .build();
  }

  private static KeySchemaElement keyElement(String attributeName, KeyType keyType) {
    return KeySchemaElement.builder().attributeName(attributeName).keyType(keyType).build();
  }
}
