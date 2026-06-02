package no.unit.nva.publication.download.utils;

import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
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

public class UriShortenerLocalDynamoDb {

  public static final String URI_MAP_PRIMARY_PARTITION_KEY = "shortenedUri";

  protected DynamoDbClient client;
  private String uriMapTableName;

  public void setUriMapTableName(String uriMapTableName) {
    this.uriMapTableName = uriMapTableName;
  }

  public void init(String uriMapTableName) {
    setUriMapTableName(uriMapTableName);
    client = DynamoDBEmbedded.create().dynamoDbClient();
    client.createTable(createTableRequest());
    updateWithTimeToLive();
  }

  private void updateWithTimeToLive() {
    var updateTimeToLiveRequest =
        UpdateTimeToLiveRequest.builder()
            .tableName(uriMapTableName)
            .timeToLiveSpecification(
                TimeToLiveSpecification.builder()
                    .attributeName("expiresDate")
                    .enabled(true)
                    .build())
            .build();
    client.updateTimeToLive(updateTimeToLiveRequest);
  }

  @AfterEach
  public void shutdown() {
    client.close();
  }

  private CreateTableRequest createTableRequest() {
    return CreateTableRequest.builder()
        .tableName(uriMapTableName)
        .attributeDefinitions(
            AttributeDefinition.builder()
                .attributeName(URI_MAP_PRIMARY_PARTITION_KEY)
                .attributeType(ScalarAttributeType.S)
                .build())
        .keySchema(
            KeySchemaElement.builder()
                .attributeName(URI_MAP_PRIMARY_PARTITION_KEY)
                .keyType(KeyType.HASH)
                .build())
        .billingMode(BillingMode.PAY_PER_REQUEST)
        .build();
  }
}
