package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/** Reads items by their keys, with consistent reads and one {@code BatchGetItem} per 100 keys. */
final class BatchItemReader {

  static final int MAX_RETRIES = 6;
  private static final int MAX_KEYS_PER_BATCH_GET = 100;
  private static final String INTERRUPTED_MESSAGE =
      "Interrupted while waiting to retry unprocessed keys";

  private final DynamoDbClient client;
  private final String tableName;
  private final Duration initialBackoff;

  BatchItemReader(DynamoDbClient client, String tableName, Duration initialBackoff) {
    this.client = client;
    this.tableName = tableName;
    this.initialBackoff = initialBackoff;
  }

  /** Returns the items that exist. A key without an item gives nothing. */
  List<Map<String, AttributeValue>> read(List<Map<String, AttributeValue>> keys) {
    var items = new ArrayList<Map<String, AttributeValue>>();
    for (var start = 0; start < keys.size(); start += MAX_KEYS_PER_BATCH_GET) {
      var batchOfKeys = keys.subList(start, Math.min(start + MAX_KEYS_PER_BATCH_GET, keys.size()));
      items.addAll(readBatch(batchOfKeys));
    }
    return items;
  }

  /**
   * Reads one batch of keys, asking again for the keys DynamoDB leaves unprocessed. Fails if keys
   * stay unprocessed, rather than report an existing item as missing.
   */
  private List<Map<String, AttributeValue>> readBatch(List<Map<String, AttributeValue>> keys) {
    var items = new ArrayList<Map<String, AttributeValue>>();
    var unprocessedKeys =
        Map.of(tableName, KeysAndAttributes.builder().keys(keys).consistentRead(true).build());
    for (var retryCount = 0; !unprocessedKeys.isEmpty(); retryCount++) {
      if (retryCount > MAX_RETRIES) {
        throw new IllegalStateException(
            "DynamoDB left keys unprocessed after %d retries".formatted(MAX_RETRIES));
      }
      waitBeforeRetry(retryCount);
      var response =
          client.batchGetItem(BatchGetItemRequest.builder().requestItems(unprocessedKeys).build());
      items.addAll(response.responses().getOrDefault(tableName, emptyList()));
      unprocessedKeys = response.unprocessedKeys();
    }
    return items;
  }

  /** Waits twice as long before each retry, and not at all before the first request. */
  @SuppressWarnings("PMD.DoNotUseThreads")
  private void waitBeforeRetry(int retryCount) {
    if (retryCount == 0) {
      return;
    }
    try {
      Thread.sleep(initialBackoff.multipliedBy(1L << (retryCount - 1)));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(INTERRUPTED_MESSAGE, exception);
    }
  }
}
