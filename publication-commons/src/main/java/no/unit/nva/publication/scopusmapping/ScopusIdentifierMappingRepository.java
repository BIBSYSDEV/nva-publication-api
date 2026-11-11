package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentMappings;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/**
 * Scopus id mappings (AUID to Cristin person, AFID to Cristin institution and unit), stored as
 * items in the import candidates table. The module README describes the item format.
 */
public class ScopusIdentifierMappingRepository {

  static final int MAX_BATCH_GET_RETRIES = 6;
  private static final int MAX_KEYS_PER_BATCH_GET = 100;
  private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(50);
  private static final String INTERRUPTED_MESSAGE =
      "Interrupted while waiting to retry unprocessed keys";

  private final DynamoDbClient client;
  private final String tableName;
  private final Duration initialBackoff;

  public ScopusIdentifierMappingRepository(DynamoDbClient client, String tableName) {
    this(client, tableName, DEFAULT_INITIAL_BACKOFF);
  }

  ScopusIdentifierMappingRepository(
      DynamoDbClient client, String tableName, Duration initialBackoff) {
    this.client = client;
    this.tableName = tableName;
    this.initialBackoff = initialBackoff;
  }

  /**
   * Returns the current, non-deleted mappings for the given Scopus ids, with consistent reads and
   * one {@code BatchGetItem} per 100 keys. Ids without a mapping, or with a deleted one, are absent
   * from the result. So are ids whose item is malformed; those are logged as errors. Duplicate ids
   * are fine, null ids are not.
   */
  public ScopusMappings fetchCurrentMappings(
      Collection<String> scopusAuthorIds, Collection<String> scopusAffiliationIds) {
    var keys =
        Stream.concat(
                distinctIds(scopusAuthorIds, "scopusAuthorIds")
                    .map(ScopusMappingItems::authorCurrentKey),
                distinctIds(scopusAffiliationIds, "scopusAffiliationIds")
                    .map(ScopusMappingItems::affiliationCurrentKey))
            .toList();
    var items = new ArrayList<Map<String, AttributeValue>>();
    for (var start = 0; start < keys.size(); start += MAX_KEYS_PER_BATCH_GET) {
      var batchOfKeys = keys.subList(start, Math.min(start + MAX_KEYS_PER_BATCH_GET, keys.size()));
      items.addAll(fetchConsistently(batchOfKeys));
    }
    return toCurrentMappings(items);
  }

  private static Stream<String> distinctIds(Collection<String> ids, String name) {
    requireNonNull(ids, name);
    return ids.stream().map(id -> requireNonNull(id, name + " must not contain null")).distinct();
  }

  /** Reads one batch of keys, asking again for the keys DynamoDB leaves unprocessed. */
  private List<Map<String, AttributeValue>> fetchConsistently(
      List<Map<String, AttributeValue>> keys) {
    var items = new ArrayList<Map<String, AttributeValue>>();
    var unprocessedKeys =
        Map.of(tableName, KeysAndAttributes.builder().keys(keys).consistentRead(true).build());
    for (var retryCount = 0; !unprocessedKeys.isEmpty(); retryCount++) {
      if (retryCount > MAX_BATCH_GET_RETRIES) {
        throw new IllegalStateException(
            "DynamoDB left keys unprocessed after %d retries".formatted(MAX_BATCH_GET_RETRIES));
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
