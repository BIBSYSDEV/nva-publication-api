package no.unit.nva.publication.service.impl;

import static no.unit.nva.publication.PublicationServiceConfig.defaultDynamoDbClient;
import static no.unit.nva.publication.storage.model.DatabaseConstants.RESOURCES_TABLE_NAME;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import java.util.Map;
import java.util.UUID;
import no.unit.nva.publication.model.business.Entity;
import no.unit.nva.publication.model.storage.Dao;
import nva.commons.core.JacocoGenerated;

public class VersionRefreshService {

  private static final String VERSION_UPDATE_EXPRESSION = "SET #version = :newVersion";
  private static final String VERSION_ATTRIBUTE_NAME = "#version";
  private static final String NEW_VERSION_VALUE = ":newVersion";

  private final AmazonDynamoDB client;
  private final String tableName;

  @JacocoGenerated
  public static VersionRefreshService defaultService() {
    return new VersionRefreshService(defaultDynamoDbClient(), RESOURCES_TABLE_NAME);
  }

  public VersionRefreshService(AmazonDynamoDB client, String tableName) {
    this.client = client;
    this.tableName = tableName;
  }

  public void refresh(Entity entity) {
    var dao = entity.toDao();
    client.updateItem(createUpdateVersionRequest(dao));
  }

  private UpdateItemRequest createUpdateVersionRequest(Dao dao) {
    return new UpdateItemRequest()
        .withTableName(tableName)
        .withKey(dao.primaryKey())
        .withUpdateExpression(VERSION_UPDATE_EXPRESSION)
        .withConditionExpression("attribute_exists(PK0) AND attribute_exists(SK0)")
        .withExpressionAttributeNames(Map.of(VERSION_ATTRIBUTE_NAME, Dao.VERSION_FIELD))
        .withExpressionAttributeValues(
            Map.of(NEW_VERSION_VALUE, new AttributeValue(UUID.randomUUID().toString())))
        .withReturnValuesOnConditionCheckFailure("ALL_OLD");
  }
}
