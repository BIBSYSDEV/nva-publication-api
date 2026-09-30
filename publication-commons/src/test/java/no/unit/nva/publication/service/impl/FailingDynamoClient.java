package no.unit.nva.publication.service.impl;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchWriteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

public class FailingDynamoClient implements DynamoDbClient {

  private static final String BATCH_WRITE_ERROR_MESSAGE = "ERRROOOOOOOOOOR";
  private final DynamoDbClient client;

  public FailingDynamoClient(DynamoDbClient client) {
    this.client = client;
  }

  @Override
  public String serviceName() {
    return this.client.serviceName();
  }

  @Override
  public void close() {
    this.client.close();
  }

  @Override
  public BatchWriteItemResponse batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
    throw DynamoDbException.builder().message(BATCH_WRITE_ERROR_MESSAGE).build();
  }

  @Override
  public BatchGetItemResponse batchGetItem(BatchGetItemRequest batchGetItemRequest) {
    return this.client.batchGetItem(batchGetItemRequest);
  }

  @Override
  public DeleteItemResponse deleteItem(DeleteItemRequest deleteItemRequest) {
    return this.client.deleteItem(deleteItemRequest);
  }

  @Override
  public GetItemResponse getItem(GetItemRequest getItemRequest) {
    return this.client.getItem(getItemRequest);
  }

  @Override
  public PutItemResponse putItem(PutItemRequest putItemRequest) {
    return this.client.putItem(putItemRequest);
  }

  @Override
  public QueryResponse query(QueryRequest queryRequest) {
    return this.client.query(queryRequest);
  }

  @Override
  public ScanResponse scan(ScanRequest scanRequest) {
    return this.client.scan(scanRequest);
  }

  @Override
  public TransactWriteItemsResponse transactWriteItems(
      TransactWriteItemsRequest transactWriteItemsRequest) {
    return this.client.transactWriteItems(transactWriteItemsRequest);
  }

  @Override
  public UpdateItemResponse updateItem(UpdateItemRequest updateItemRequest) {
    return this.client.updateItem(updateItemRequest);
  }
}
