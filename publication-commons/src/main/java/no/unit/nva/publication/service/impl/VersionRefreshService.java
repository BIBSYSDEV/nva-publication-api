package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationServiceConfig.defaultDynamoDbClient;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;

import java.util.Map;
import java.util.UUID;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.Dao;
import nva.commons.core.JacocoGenerated;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

public class VersionRefreshService {

  private static final String VERSION_UPDATE_EXPRESSION = "SET #version = :newVersion";
  private static final String VERSION_ATTRIBUTE_NAME = "#version";
  private static final String NEW_VERSION_VALUE = ":newVersion";

  private final DynamoDbClient client;
  private final String tableName;

  @JacocoGenerated
  public static VersionRefreshService defaultService() {
    return new VersionRefreshService(defaultDynamoDbClient(), RESOURCES_TABLE_NAME);
  }

  public VersionRefreshService(DynamoDbClient client, String tableName) {
    this.client = client;
    this.tableName = tableName;
  }

  public void refresh(Entity entity) {
    var dao = entity.toDao();
    client.updateItem(createUpdateVersionRequest(dao));
  }

  private UpdateItemRequest createUpdateVersionRequest(Dao dao) {
    return UpdateItemRequest.builder()
        .tableName(tableName)
        .key(dao.primaryKey())
        .updateExpression(VERSION_UPDATE_EXPRESSION)
        .conditionExpression("attribute_exists(PK0) AND attribute_exists(SK0)")
        .expressionAttributeNames(Map.of(VERSION_ATTRIBUTE_NAME, Dao.VERSION_FIELD))
        .expressionAttributeValues(
            Map.of(NEW_VERSION_VALUE, AttributeValue.fromS(UUID.randomUUID().toString())))
        .returnValuesOnConditionCheckFailure("ALL_OLD")
        .build();
  }
}
