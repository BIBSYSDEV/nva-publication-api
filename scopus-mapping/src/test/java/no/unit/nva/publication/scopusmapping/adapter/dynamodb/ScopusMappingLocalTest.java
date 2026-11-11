package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import java.util.Map;
import no.unit.nva.publication.scopusmapping.application.ScopusMappingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Runs the use cases against the DynamoDB adapter and a local DynamoDB table with the same keys as
 * the import candidates table.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
abstract class ScopusMappingLocalTest {

  static final String TABLE_NAME = "import-candidates";

  protected DynamoDbClient client;
  protected ScopusMappingService service;

  @BeforeEach
  void createTableAndService() {
    client = DynamoDBEmbedded.create(null, true).dynamoDbClient();
    client.createTable(
        CreateTableRequest.builder()
            .tableName(TABLE_NAME)
            .attributeDefinitions(
                stringAttribute(PRIMARY_KEY_PARTITION_KEY_NAME),
                stringAttribute(PRIMARY_KEY_SORT_KEY_NAME))
            .keySchema(
                keyElement(PRIMARY_KEY_PARTITION_KEY_NAME, KeyType.HASH),
                keyElement(PRIMARY_KEY_SORT_KEY_NAME, KeyType.RANGE))
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build());
    service = new ScopusMappingService(new DynamoDbScopusMappingRepository(client, TABLE_NAME));
  }

  @AfterEach
  void closeClient() {
    client.close();
  }

  protected void putItem(Map<String, AttributeValue> item) {
    client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
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
