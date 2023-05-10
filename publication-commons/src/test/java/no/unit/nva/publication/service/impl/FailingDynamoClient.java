package no.unit.nva.publication.service.impl;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.regions.Region;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.BatchExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.BatchExecuteStatementResult;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchWriteItemResult;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateBackupRequest;
import com.amazonaws.services.dynamodbv2.model.CreateBackupResult;
import com.amazonaws.services.dynamodbv2.model.CreateGlobalTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateGlobalTableResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteBackupRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteBackupResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeBackupRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeBackupResult;
import com.amazonaws.services.dynamodbv2.model.DescribeContinuousBackupsRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeContinuousBackupsResult;
import com.amazonaws.services.dynamodbv2.model.DescribeContributorInsightsRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeContributorInsightsResult;
import com.amazonaws.services.dynamodbv2.model.DescribeEndpointsRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeEndpointsResult;
import com.amazonaws.services.dynamodbv2.model.DescribeExportRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeExportResult;
import com.amazonaws.services.dynamodbv2.model.DescribeGlobalTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeGlobalTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeGlobalTableSettingsRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeGlobalTableSettingsResult;
import com.amazonaws.services.dynamodbv2.model.DescribeImportRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeImportResult;
import com.amazonaws.services.dynamodbv2.model.DescribeKinesisStreamingDestinationRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeKinesisStreamingDestinationResult;
import com.amazonaws.services.dynamodbv2.model.DescribeLimitsRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeLimitsResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableReplicaAutoScalingRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableReplicaAutoScalingResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableResult;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTimeToLiveResult;
import com.amazonaws.services.dynamodbv2.model.DisableKinesisStreamingDestinationRequest;
import com.amazonaws.services.dynamodbv2.model.DisableKinesisStreamingDestinationResult;
import com.amazonaws.services.dynamodbv2.model.EnableKinesisStreamingDestinationRequest;
import com.amazonaws.services.dynamodbv2.model.EnableKinesisStreamingDestinationResult;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteStatementResult;
import com.amazonaws.services.dynamodbv2.model.ExecuteTransactionRequest;
import com.amazonaws.services.dynamodbv2.model.ExecuteTransactionResult;
import com.amazonaws.services.dynamodbv2.model.ExportTableToPointInTimeRequest;
import com.amazonaws.services.dynamodbv2.model.ExportTableToPointInTimeResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ImportTableRequest;
import com.amazonaws.services.dynamodbv2.model.ImportTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import com.amazonaws.services.dynamodbv2.model.ListBackupsRequest;
import com.amazonaws.services.dynamodbv2.model.ListBackupsResult;
import com.amazonaws.services.dynamodbv2.model.ListContributorInsightsRequest;
import com.amazonaws.services.dynamodbv2.model.ListContributorInsightsResult;
import com.amazonaws.services.dynamodbv2.model.ListExportsRequest;
import com.amazonaws.services.dynamodbv2.model.ListExportsResult;
import com.amazonaws.services.dynamodbv2.model.ListGlobalTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListGlobalTablesResult;
import com.amazonaws.services.dynamodbv2.model.ListImportsRequest;
import com.amazonaws.services.dynamodbv2.model.ListImportsResult;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ListTagsOfResourceRequest;
import com.amazonaws.services.dynamodbv2.model.ListTagsOfResourceResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.RestoreTableFromBackupRequest;
import com.amazonaws.services.dynamodbv2.model.RestoreTableFromBackupResult;
import com.amazonaws.services.dynamodbv2.model.RestoreTableToPointInTimeRequest;
import com.amazonaws.services.dynamodbv2.model.RestoreTableToPointInTimeResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TagResourceRequest;
import com.amazonaws.services.dynamodbv2.model.TagResourceResult;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactGetItemsResult;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsRequest;
import com.amazonaws.services.dynamodbv2.model.TransactWriteItemsResult;
import com.amazonaws.services.dynamodbv2.model.UntagResourceRequest;
import com.amazonaws.services.dynamodbv2.model.UntagResourceResult;
import com.amazonaws.services.dynamodbv2.model.UpdateContinuousBackupsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateContinuousBackupsResult;
import com.amazonaws.services.dynamodbv2.model.UpdateContributorInsightsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateContributorInsightsResult;
import com.amazonaws.services.dynamodbv2.model.UpdateGlobalTableRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateGlobalTableResult;
import com.amazonaws.services.dynamodbv2.model.UpdateGlobalTableSettingsRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateGlobalTableSettingsResult;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateItemResult;
import com.amazonaws.services.dynamodbv2.model.UpdateTableReplicaAutoScalingRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTableReplicaAutoScalingResult;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTableResult;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveResult;
import com.amazonaws.services.dynamodbv2.model.WriteRequest;
import com.amazonaws.services.dynamodbv2.waiters.AmazonDynamoDBWaiters;
import java.util.List;
import java.util.Map;

public class FailingDynamoClient implements AmazonDynamoDB {

  private static final String BATCH_WRITE_ERROR_MESSAGE = "ERRROOOOOOOOOOR";
  private final AmazonDynamoDB client;

  public FailingDynamoClient(AmazonDynamoDB dynamoDB) {
    this.client = dynamoDB;
  }

  @Override
  public void setEndpoint(String endpoint) {
    this.client.setEndpoint(endpoint);
  }

  @Override
  public void setRegion(Region region) {
    this.client.setRegion(region);
  }

  @Override
  public BatchExecuteStatementResult batchExecuteStatement(
    BatchExecuteStatementRequest batchExecuteStatementRequest) {
    return this.client.batchExecuteStatement(batchExecuteStatementRequest);
  }

  @Override
  public BatchGetItemResult batchGetItem(BatchGetItemRequest batchGetItemRequest) {
    return this.client.batchGetItem(batchGetItemRequest);
  }

  @Override
  public BatchGetItemResult batchGetItem(Map<String, KeysAndAttributes> requestItems,
    String returnConsumedCapacity) {
    return this.client.batchGetItem(requestItems, returnConsumedCapacity);
  }

  @Override
  public BatchGetItemResult batchGetItem(Map<String, KeysAndAttributes> requestItems) {
    return this.client.batchGetItem(requestItems);
  }

  @Override
  public BatchWriteItemResult batchWriteItem(BatchWriteItemRequest batchWriteItemRequest) {
    throw new AmazonDynamoDBException(BATCH_WRITE_ERROR_MESSAGE);
  }

  @Override
  public BatchWriteItemResult batchWriteItem(Map<String, List<WriteRequest>> requestItems) {
    return this.client.batchWriteItem(requestItems);
  }

  @Override
  public CreateBackupResult createBackup(CreateBackupRequest createBackupRequest) {
    return this.client.createBackup(createBackupRequest);
  }

  @Override
  public CreateGlobalTableResult createGlobalTable(
    CreateGlobalTableRequest createGlobalTableRequest) {
    return this.client.createGlobalTable(createGlobalTableRequest);
  }

  @Override
  public CreateTableResult createTable(CreateTableRequest createTableRequest) {
    return this.client.createTable(createTableRequest);
  }

  @Override
  public CreateTableResult createTable(List<AttributeDefinition> attributeDefinitions,
    String tableName, List<KeySchemaElement> keySchema,
    ProvisionedThroughput provisionedThroughput) {
    return this.client.createTable(attributeDefinitions, tableName, keySchema,
      provisionedThroughput);
  }

  @Override
  public DeleteBackupResult deleteBackup(DeleteBackupRequest deleteBackupRequest) {
    return this.client.deleteBackup(deleteBackupRequest);
  }

  @Override
  public DeleteItemResult deleteItem(DeleteItemRequest deleteItemRequest) {
    return this.client.deleteItem(deleteItemRequest);
  }

  @Override
  public DeleteItemResult deleteItem(String tableName, Map<String, AttributeValue> key) {
    return this.client.deleteItem(tableName, key);
  }

  @Override
  public DeleteItemResult deleteItem(String tableName, Map<String, AttributeValue> key,
    String returnValues) {
    return this.client.deleteItem(tableName, key);
  }

  @Override
  public DeleteTableResult deleteTable(DeleteTableRequest deleteTableRequest) {
    return this.client.deleteTable(deleteTableRequest);
  }

  @Override
  public DeleteTableResult deleteTable(String tableName) {
    return this.client.deleteTable(tableName);
  }

  @Override
  public DescribeBackupResult describeBackup(DescribeBackupRequest describeBackupRequest) {
    return this.client.describeBackup(describeBackupRequest);
  }

  @Override
  public DescribeContinuousBackupsResult describeContinuousBackups(
    DescribeContinuousBackupsRequest describeContinuousBackupsRequest) {
    return this.client.describeContinuousBackups(describeContinuousBackupsRequest);
  }

  @Override
  public DescribeContributorInsightsResult describeContributorInsights(
    DescribeContributorInsightsRequest describeContributorInsightsRequest) {
    return this.client.describeContributorInsights(describeContributorInsightsRequest);
  }

  @Override
  public DescribeEndpointsResult describeEndpoints(
    DescribeEndpointsRequest describeEndpointsRequest) {
    return this.client.describeEndpoints(describeEndpointsRequest);
  }

  @Override
  public DescribeExportResult describeExport(DescribeExportRequest describeExportRequest) {
    return this.client.describeExport(describeExportRequest);
  }

  @Override
  public DescribeGlobalTableResult describeGlobalTable(
    DescribeGlobalTableRequest describeGlobalTableRequest) {
    return this.client.describeGlobalTable(describeGlobalTableRequest);
  }

  @Override
  public DescribeGlobalTableSettingsResult describeGlobalTableSettings(
    DescribeGlobalTableSettingsRequest describeGlobalTableSettingsRequest) {
    return this.client.describeGlobalTableSettings(describeGlobalTableSettingsRequest);
  }

  @Override
  public DescribeImportResult describeImport(DescribeImportRequest describeImportRequest) {
    return this.client.describeImport(describeImportRequest);
  }

  @Override
  public DescribeKinesisStreamingDestinationResult describeKinesisStreamingDestination(
    DescribeKinesisStreamingDestinationRequest describeKinesisStreamingDestinationRequest) {
    return this.client.describeKinesisStreamingDestination(
      describeKinesisStreamingDestinationRequest);
  }

  @Override
  public DescribeLimitsResult describeLimits(DescribeLimitsRequest describeLimitsRequest) {
    return this.client.describeLimits(describeLimitsRequest);
  }

  @Override
  public DescribeTableResult describeTable(DescribeTableRequest describeTableRequest) {
    return this.client.describeTable(describeTableRequest);
  }

  @Override
  public DescribeTableResult describeTable(String tableName) {
    return this.client.describeTable(tableName);
  }

  @Override
  public DescribeTableReplicaAutoScalingResult describeTableReplicaAutoScaling(
    DescribeTableReplicaAutoScalingRequest describeTableReplicaAutoScalingRequest) {
    return this.client.describeTableReplicaAutoScaling(describeTableReplicaAutoScalingRequest);
  }

  @Override
  public DescribeTimeToLiveResult describeTimeToLive(
    DescribeTimeToLiveRequest describeTimeToLiveRequest) {
    return this.client.describeTimeToLive(describeTimeToLiveRequest);
  }

  @Override
  public DisableKinesisStreamingDestinationResult disableKinesisStreamingDestination(
    DisableKinesisStreamingDestinationRequest disableKinesisStreamingDestinationRequest) {
    return this.client.disableKinesisStreamingDestination(
      disableKinesisStreamingDestinationRequest);
  }

  @Override
  public EnableKinesisStreamingDestinationResult enableKinesisStreamingDestination(
    EnableKinesisStreamingDestinationRequest enableKinesisStreamingDestinationRequest) {
    return this.client.enableKinesisStreamingDestination(enableKinesisStreamingDestinationRequest);
  }

  @Override
  public ExecuteStatementResult executeStatement(ExecuteStatementRequest executeStatementRequest) {
    return this.client.executeStatement(executeStatementRequest);
  }

  @Override
  public ExecuteTransactionResult executeTransaction(
    ExecuteTransactionRequest executeTransactionRequest) {
    return this.client.executeTransaction(executeTransactionRequest);
  }

  @Override
  public ExportTableToPointInTimeResult exportTableToPointInTime(
    ExportTableToPointInTimeRequest exportTableToPointInTimeRequest) {
    return this.client.exportTableToPointInTime(exportTableToPointInTimeRequest);
  }

  @Override
  public GetItemResult getItem(GetItemRequest getItemRequest) {
    return this.client.getItem(getItemRequest);
  }

  @Override
  public GetItemResult getItem(String tableName, Map<String, AttributeValue> key) {
    return this.client.getItem(tableName, key);
  }

  @Override
  public GetItemResult getItem(String tableName, Map<String, AttributeValue> key,
    Boolean consistentRead) {
    return this.client.getItem(tableName, key, consistentRead);
  }

  @Override
  public ImportTableResult importTable(ImportTableRequest importTableRequest) {
    return this.client.importTable(importTableRequest);
  }

  @Override
  public ListBackupsResult listBackups(ListBackupsRequest listBackupsRequest) {
    return this.client.listBackups(listBackupsRequest);
  }

  @Override
  public ListContributorInsightsResult listContributorInsights(
    ListContributorInsightsRequest listContributorInsightsRequest) {
    return this.client.listContributorInsights(listContributorInsightsRequest);
  }

  @Override
  public ListExportsResult listExports(ListExportsRequest listExportsRequest) {
    return this.client.listExports(listExportsRequest);
  }

  @Override
  public ListGlobalTablesResult listGlobalTables(ListGlobalTablesRequest listGlobalTablesRequest) {
    return this.client.listGlobalTables(listGlobalTablesRequest);
  }

  @Override
  public ListImportsResult listImports(ListImportsRequest listImportsRequest) {
    return this.client.listImports(listImportsRequest);
  }

  @Override
  public ListTablesResult listTables(ListTablesRequest listTablesRequest) {
    return this.client.listTables(listTablesRequest);
  }

  @Override
  public ListTablesResult listTables() {
    return this.client.listTables();
  }

  @Override
  public ListTablesResult listTables(String exclusiveStartTableName) {
    return this.client.listTables();
  }

  @Override
  public ListTablesResult listTables(String exclusiveStartTableName, Integer limit) {
    return this.client.listTables(exclusiveStartTableName, limit);
  }

  @Override
  public ListTablesResult listTables(Integer limit) {
    return this.client.listTables(limit);
  }

  @Override
  public ListTagsOfResourceResult listTagsOfResource(
    ListTagsOfResourceRequest listTagsOfResourceRequest) {
    return this.client.listTagsOfResource(listTagsOfResourceRequest);
  }

  @Override
  public PutItemResult putItem(PutItemRequest putItemRequest) {
    return this.client.putItem(putItemRequest);
  }

  @Override
  public PutItemResult putItem(String tableName, Map<String, AttributeValue> item) {
    return this.client.putItem(tableName, item);
  }

  @Override
  public PutItemResult putItem(String tableName, Map<String, AttributeValue> item,
    String returnValues) {
    return this.client.putItem(tableName, item, returnValues);
  }

  @Override
  public QueryResult query(QueryRequest queryRequest) {
    return this.client.query(queryRequest);
  }

  @Override
  public RestoreTableFromBackupResult restoreTableFromBackup(
    RestoreTableFromBackupRequest restoreTableFromBackupRequest) {
    return this.client.restoreTableFromBackup(restoreTableFromBackupRequest);
  }

  @Override
  public RestoreTableToPointInTimeResult restoreTableToPointInTime(
    RestoreTableToPointInTimeRequest restoreTableToPointInTimeRequest) {
    return this.client.restoreTableToPointInTime(restoreTableToPointInTimeRequest);
  }

  @Override
  public ScanResult scan(ScanRequest scanRequest) {
    return this.client.scan(scanRequest);
  }

  @Override
  public ScanResult scan(String tableName, List<String> attributesToGet) {
    return this.client.scan(tableName, attributesToGet);
  }

  @Override
  public ScanResult scan(String tableName, Map<String, Condition> scanFilter) {
    return this.client.scan(tableName, scanFilter);
  }

  @Override
  public ScanResult scan(String tableName, List<String> attributesToGet,
    Map<String, Condition> scanFilter) {
    return this.client.scan(tableName, attributesToGet);
  }

  @Override
  public TagResourceResult tagResource(TagResourceRequest tagResourceRequest) {
    return this.client.tagResource(tagResourceRequest);
  }

  @Override
  public TransactGetItemsResult transactGetItems(TransactGetItemsRequest transactGetItemsRequest) {
    return this.client.transactGetItems(transactGetItemsRequest);
  }

  @Override
  public TransactWriteItemsResult transactWriteItems(
    TransactWriteItemsRequest transactWriteItemsRequest) {
    return this.client.transactWriteItems(transactWriteItemsRequest);
  }

  @Override
  public UntagResourceResult untagResource(UntagResourceRequest untagResourceRequest) {
    return this.client.untagResource(untagResourceRequest);
  }

  @Override
  public UpdateContinuousBackupsResult updateContinuousBackups(
    UpdateContinuousBackupsRequest updateContinuousBackupsRequest) {
    return this.client.updateContinuousBackups(updateContinuousBackupsRequest);
  }

  @Override
  public UpdateContributorInsightsResult updateContributorInsights(
    UpdateContributorInsightsRequest updateContributorInsightsRequest) {
    return this.client.updateContributorInsights(updateContributorInsightsRequest);
  }

  @Override
  public UpdateGlobalTableResult updateGlobalTable(
    UpdateGlobalTableRequest updateGlobalTableRequest) {
    return this.client.updateGlobalTable(updateGlobalTableRequest);
  }

  @Override
  public UpdateGlobalTableSettingsResult updateGlobalTableSettings(
    UpdateGlobalTableSettingsRequest updateGlobalTableSettingsRequest) {
    return this.client.updateGlobalTableSettings(updateGlobalTableSettingsRequest);
  }

  @Override
  public UpdateItemResult updateItem(UpdateItemRequest updateItemRequest) {
    return this.client.updateItem(updateItemRequest);
  }

  @Override
  public UpdateItemResult updateItem(String tableName, Map<String, AttributeValue> key,
    Map<String, AttributeValueUpdate> attributeUpdates) {
    return this.client.updateItem(tableName, key, attributeUpdates);
  }

  @Override
  public UpdateItemResult updateItem(String tableName, Map<String, AttributeValue> key,
    Map<String, AttributeValueUpdate> attributeUpdates, String returnValues) {
    return this.client.updateItem(tableName, key, attributeUpdates, returnValues);
  }

  @Override
  public UpdateTableResult updateTable(UpdateTableRequest updateTableRequest) {
    return this.client.updateTable(updateTableRequest);
  }

  @Override
  public UpdateTableResult updateTable(String tableName,
    ProvisionedThroughput provisionedThroughput) {
    return this.client.updateTable(tableName, provisionedThroughput);
  }

  @Override
  public UpdateTableReplicaAutoScalingResult updateTableReplicaAutoScaling(
    UpdateTableReplicaAutoScalingRequest updateTableReplicaAutoScalingRequest) {
    return this.client.updateTableReplicaAutoScaling(updateTableReplicaAutoScalingRequest);
  }

  @Override
  public UpdateTimeToLiveResult updateTimeToLive(UpdateTimeToLiveRequest updateTimeToLiveRequest) {
    return this.client.updateTimeToLive(updateTimeToLiveRequest);
  }

  @Override
  public void shutdown() {
    this.client.shutdown();
  }

  @Override
  public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
    return this.client.getCachedResponseMetadata(request);
  }

  @Override
  public AmazonDynamoDBWaiters waiters() {
    return this.client.waiters();
  }
}
