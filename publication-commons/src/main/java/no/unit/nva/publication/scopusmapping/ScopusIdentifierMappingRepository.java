package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.HISTORY_SORT_KEY_PREFIX;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.REVISION;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.affiliationPartitionKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.authorCurrentKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.authorPartitionKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.parseOrLogAndSkip;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentMappings;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toHistoryEntry;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toHistoryItem;
import static no.unit.nva.publication.scopusmapping.StoredAuthorMapping.NO_REVISION;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

/**
 * Scopus id mappings (AUID to Cristin person, AFID to Cristin institution and unit), stored as
 * items in the import candidates table. The module README describes the item format.
 */
public class ScopusIdentifierMappingRepository {

  static final int MAX_BATCH_GET_RETRIES = 6;
  private static final int MAX_KEYS_PER_BATCH_GET = 100;
  private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(50);
  private static final String REVISION_NAME_PLACEHOLDER = "#revision";
  private static final String PARTITION_KEY_VALUE_PLACEHOLDER = ":partitionKey";
  private static final String HISTORY_PREFIX_VALUE_PLACEHOLDER = ":historyPrefix";
  private static final String EXPECTED_REVISION_VALUE_PLACEHOLDER = ":expectedRevision";
  private static final String ITEM_DOES_NOT_EXIST_CONDITION =
      "attribute_not_exists(" + PRIMARY_KEY_PARTITION_KEY_NAME + ")";
  private static final String REVISION_MATCHES_CONDITION =
      REVISION_NAME_PLACEHOLDER + " = " + EXPECTED_REVISION_VALUE_PLACEHOLDER;
  private static final String HISTORY_KEY_CONDITION =
      "%s = %s AND begins_with(%s, %s)"
          .formatted(
              PRIMARY_KEY_PARTITION_KEY_NAME,
              PARTITION_KEY_VALUE_PLACEHOLDER,
              PRIMARY_KEY_SORT_KEY_NAME,
              HISTORY_PREFIX_VALUE_PLACEHOLDER);
  private static final String INTERRUPTED_MESSAGE =
      "Interrupted while waiting to retry unprocessed keys";
  private static final String NO_EXPECTED_REVISION = "none";

  private final DynamoDbClient client;
  private final String tableName;
  private final Clock clock;
  private final Duration initialBackoff;

  public ScopusIdentifierMappingRepository(DynamoDbClient client, String tableName) {
    this(client, tableName, Clock.systemUTC(), DEFAULT_INITIAL_BACKOFF);
  }

  ScopusIdentifierMappingRepository(
      DynamoDbClient client, String tableName, Clock clock, Duration initialBackoff) {
    this.client = client;
    this.tableName = tableName;
    this.clock = clock;
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

  /**
   * Maps a Scopus author id to a Cristin person, as a curator correction. The stored mapping must
   * have {@code expectedRevision}, or, when it is empty, there must be no mapping (a deleted
   * mapping counts as none). A new {@code HISTORY} item is written and {@code CURRENT} replaced in
   * one transaction, conditioned on the same revision. Saving the person the author is already
   * mapped to is a confirmation: it writes a new version like any other correction, which clears
   * {@code needsReview}.
   *
   * @throws ScopusMappingConflictException if the stored mapping does not have the expected
   *     revision
   */
  public ScopusAuthorMapping saveCristinPersonId(
      String scopusAuthorId,
      String cristinPersonId,
      ScopusCorrectionContext correctionContext,
      String username,
      Optional<Integer> expectedRevision)
      throws ScopusMappingConflictException {
    var storedMapping = fetchStoredAuthorMapping(scopusAuthorId);
    if (!storedMapping.hasExpectedRevision(expectedRevision)) {
      throw new ScopusMappingConflictException(
          "Scopus author mapping %s is not at the expected revision (%s)"
              .formatted(
                  scopusAuthorId,
                  expectedRevision.map(String::valueOf).orElse(NO_EXPECTED_REVISION)));
    }
    var newMapping =
        new ScopusAuthorMapping(
            scopusAuthorId,
            cristinPersonId,
            storedMapping.scopusName(),
            storedMapping.orcid(),
            null,
            null,
            false,
            ScopusMappingSource.CURATOR,
            clock.instant().truncatedTo(ChronoUnit.MILLIS),
            username,
            storedMapping.revision() + 1,
            false);
    writeNewVersion(newMapping, correctionContext, storedMapping.revision());
    return newMapping;
  }

  /**
   * Returns every written version of an author mapping, oldest first. A malformed version is logged
   * as an error and left out.
   */
  public List<ScopusMappingHistoryEntry<ScopusAuthorMapping>> fetchAuthorMappingHistory(
      String scopusAuthorId) {
    return fetchHistoryOldestFirst(
        authorPartitionKey(scopusAuthorId),
        ScopusMappingItems::toAuthorMapping,
        ScopusAuthorMapping::revision);
  }

  /**
   * Returns every written version of an affiliation mapping, oldest first. A malformed version is
   * logged as an error and left out.
   */
  public List<ScopusMappingHistoryEntry<ScopusAffiliationMapping>> fetchAffiliationMappingHistory(
      String scopusAffiliationId) {
    return fetchHistoryOldestFirst(
        affiliationPartitionKey(scopusAffiliationId),
        ScopusMappingItems::toAffiliationMapping,
        ScopusAffiliationMapping::revision);
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

  private StoredAuthorMapping fetchStoredAuthorMapping(String scopusAuthorId) {
    var response =
        client.getItem(
            GetItemRequest.builder()
                .tableName(tableName)
                .key(authorCurrentKey(scopusAuthorId))
                .consistentRead(true)
                .build());
    return response.hasItem()
        ? StoredAuthorMapping.fromItem(response.item())
        : StoredAuthorMapping.NONE;
  }

  private void writeNewVersion(
      ScopusAuthorMapping mapping,
      ScopusCorrectionContext correctionContext,
      int replacedRevision) {
    var historyPut = putIfItemDoesNotExist(toHistoryItem(mapping, correctionContext));
    var currentPut =
        replacedRevision == NO_REVISION
            ? putIfItemDoesNotExist(toCurrentItem(mapping))
            : putIfRevisionMatches(toCurrentItem(mapping), replacedRevision);
    var request =
        TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder().put(historyPut).build(),
                TransactWriteItem.builder().put(currentPut).build())
            .build();
    client.transactWriteItems(request);
  }

  private Put putIfItemDoesNotExist(Map<String, AttributeValue> item) {
    return Put.builder()
        .tableName(tableName)
        .item(item)
        .conditionExpression(ITEM_DOES_NOT_EXIST_CONDITION)
        .build();
  }

  private Put putIfRevisionMatches(Map<String, AttributeValue> item, int expectedRevision) {
    return Put.builder()
        .tableName(tableName)
        .item(item)
        .conditionExpression(REVISION_MATCHES_CONDITION)
        .expressionAttributeNames(Map.of(REVISION_NAME_PLACEHOLDER, REVISION))
        .expressionAttributeValues(
            Map.of(
                EXPECTED_REVISION_VALUE_PLACEHOLDER,
                AttributeValue.fromN(String.valueOf(expectedRevision))))
        .build();
  }

  /**
   * Reads consistently, so history shown right after a correction includes it. The cost is small:
   * one id, a handful of small items, read rarely. Orders by revision, since the sort key starts
   * with the timestamp and clocks can disagree.
   */
  private <T> List<ScopusMappingHistoryEntry<T>> fetchHistoryOldestFirst(
      String partitionKey,
      Function<Map<String, AttributeValue>, T> mappingReader,
      ToIntFunction<T> revisionOf) {
    return client
        .queryPaginator(
            QueryRequest.builder()
                .tableName(tableName)
                .consistentRead(true)
                .keyConditionExpression(HISTORY_KEY_CONDITION)
                .expressionAttributeValues(
                    Map.of(
                        PARTITION_KEY_VALUE_PLACEHOLDER,
                        AttributeValue.fromS(partitionKey),
                        HISTORY_PREFIX_VALUE_PLACEHOLDER,
                        AttributeValue.fromS(HISTORY_SORT_KEY_PREFIX)))
                .build())
        .items()
        .stream()
        .flatMap(
            item ->
                parseOrLogAndSkip(item, historyItem -> toHistoryEntry(historyItem, mappingReader))
                    .stream())
        .sorted(Comparator.comparingInt(entry -> revisionOf.applyAsInt(entry.mapping())))
        .toList();
  }
}
