package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Gatherers;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/** Reads items by their keys, with consistent reads and one {@code BatchGetItem} per 100 keys. */
final class BatchItemReader {

  private static final int MAX_KEYS_PER_BATCH_GET = 100;

  private final DynamoDbClient client;
  private final String tableName;

  BatchItemReader(DynamoDbClient client, String tableName) {
    this.client = client;
    this.tableName = tableName;
  }

  /**
   * Returns the items that exist. A key without an item gives nothing. A key given more than once
   * is read once, since {@code BatchGetItem} rejects duplicate keys.
   */
  List<Map<String, AttributeValue>> read(Collection<Map<String, AttributeValue>> keys) {
    return keys.stream()
        .distinct()
        .gather(Gatherers.windowFixed(MAX_KEYS_PER_BATCH_GET))
        .map(this::readBatch)
        .flatMap(List::stream)
        .toList();
  }

  /**
   * Reads one batch of keys. Fails if DynamoDB leaves keys unprocessed, rather than report an
   * existing item as missing.
   */
  private List<Map<String, AttributeValue>> readBatch(List<Map<String, AttributeValue>> keys) {
    var requestedKeys =
        Map.of(tableName, KeysAndAttributes.builder().keys(keys).consistentRead(true).build());
    var response =
        client.batchGetItem(BatchGetItemRequest.builder().requestItems(requestedKeys).build());
    if (!response.unprocessedKeys().isEmpty()) {
      throw new IllegalStateException("DynamoDB left keys unprocessed");
    }
    return response.responses().getOrDefault(tableName, emptyList());
  }
}
