package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static no.unit.nva.publication.scopusmapping.ScopusMappingIds.requireNumericId;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.HISTORY_SORT_KEY_PREFIX;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.REVISION;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.affiliationPartitionKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.authorCurrentKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.authorPartitionKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentMappings;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toHistoryEntry;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toHistoryItem;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Scopus id mappings (AUID to Cristin person, AFID to Cristin institution and unit), stored as
 * items in the import candidates table. The module README describes the item format.
 */
public class ScopusIdentifierMappingRepository {

  private static final int MAX_KEYS_PER_BATCH_GET = 100;
  private static final int MAX_BATCH_GET_RETRIES = 7;
  private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(50);
  private static final Duration MAX_BACKOFF = Duration.ofSeconds(2);
  private static final int NO_REVISION = 0;
  private static final String CONDITIONAL_CHECK_FAILED = "ConditionalCheckFailed";
  private static final String PARTITION_KEY_NAME_PLACEHOLDER = "#partitionKey";
  private static final String SORT_KEY_NAME_PLACEHOLDER = "#sortKey";
  private static final String REVISION_NAME_PLACEHOLDER = "#revision";
  private static final String PARTITION_KEY_VALUE_PLACEHOLDER = ":partitionKey";
  private static final String HISTORY_PREFIX_VALUE_PLACEHOLDER = ":historyPrefix";
  private static final String EXPECTED_REVISION_VALUE_PLACEHOLDER = ":expectedRevision";
  private static final String ITEM_DOES_NOT_EXIST_CONDITION =
      "attribute_not_exists(" + PARTITION_KEY_NAME_PLACEHOLDER + ")";
  private static final String REVISION_MATCHES_CONDITION =
      REVISION_NAME_PLACEHOLDER + " = " + EXPECTED_REVISION_VALUE_PLACEHOLDER;
  private static final String HISTORY_KEY_CONDITION =
      PARTITION_KEY_NAME_PLACEHOLDER
          + " = "
          + PARTITION_KEY_VALUE_PLACEHOLDER
          + " AND begins_with("
          + SORT_KEY_NAME_PLACEHOLDER
          + ", "
          + HISTORY_PREFIX_VALUE_PLACEHOLDER
          + ")";
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
   * from the result. So are ids whose item is malformed; those are logged as errors.
   */
  public ScopusMappings fetchCurrentMappings(
      Collection<String> scopusAuthorIds, Collection<String> scopusAffiliationIds) {
    requireNonNull(scopusAuthorIds, "scopusAuthorIds");
    requireNonNull(scopusAffiliationIds, "scopusAffiliationIds");
    var keys =
        Stream.concat(
                distinctIds(scopusAuthorIds).map(ScopusMappingItems::authorCurrentKey),
                distinctIds(scopusAffiliationIds).map(ScopusMappingItems::affiliationCurrentKey))
            .toList();
    var items =
        IntStream.iterate(0, start -> start < keys.size(), start -> start + MAX_KEYS_PER_BATCH_GET)
            .mapToObj(
                start -> keys.subList(start, Math.min(start + MAX_KEYS_PER_BATCH_GET, keys.size())))
            .flatMap(batchOfKeys -> fetchConsistently(batchOfKeys).stream())
            .toList();
    return toCurrentMappings(items);
  }

  /**
   * Maps a Scopus author id to a Cristin person, as a curator correction. The stored mapping must
   * have {@code expectedRevision}, or, when it is empty, there must be no mapping (a deleted or
   * malformed mapping counts as none, and is replaced). If the author is already mapped to the
   * person, nothing is written and the stored mapping is returned. Otherwise a new {@code HISTORY}
   * item is written and {@code CURRENT} replaced in one transaction, conditioned on the same
   * revision.
   *
   * @throws ScopusMappingConflictException if the stored mapping does not have the expected
   *     revision, changes before the write, or is malformed without a readable revision
   */
  public ScopusAuthorMapping saveCristinPersonId(
      String scopusAuthorId,
      String cristinPersonId,
      ScopusCorrectionContext correctionContext,
      String username,
      Optional<Integer> expectedRevision)
      throws ScopusMappingConflictException {
    requireNumericId(scopusAuthorId, "scopusAuthorId");
    requireNumericId(cristinPersonId, "cristinPersonId");
    requireNonNull(correctionContext, "correctionContext");
    requireNonNull(username, "username");
    requireNonNull(expectedRevision, "expectedRevision");
    var storedMapping = fetchStoredAuthorMapping(scopusAuthorId);
    if (!storedMapping.hasExpectedRevision(expectedRevision)) {
      throw new ScopusMappingConflictException(
          "Scopus author mapping %s is not at the expected revision (%s)"
              .formatted(
                  scopusAuthorId,
                  expectedRevision.map(String::valueOf).orElse(NO_EXPECTED_REVISION)));
    }
    var unchangedMapping = storedMapping.liveMappingTo(cristinPersonId);
    if (unchangedMapping.isPresent()) {
      return unchangedMapping.get();
    }
    var replacedRevision = storedMapping.revision();
    var newMapping =
        newCuratorMapping(
            scopusAuthorId, cristinPersonId, username, storedMapping.mapping(), replacedRevision);
    writeNewVersion(newMapping, correctionContext, replacedRevision);
    return newMapping;
  }

  /** Returns every written version of an author mapping, oldest first. */
  public List<ScopusMappingHistoryEntry<ScopusAuthorMapping>> fetchAuthorMappingHistory(
      String scopusAuthorId) {
    return fetchHistoryItemsOldestFirst(authorPartitionKey(scopusAuthorId)).stream()
        .map(item -> toHistoryEntry(item, ScopusMappingItems::toAuthorMapping))
        .toList();
  }

  /** Returns every written version of an affiliation mapping, oldest first. */
  public List<ScopusMappingHistoryEntry<ScopusAffiliationMapping>> fetchAffiliationMappingHistory(
      String scopusAffiliationId) {
    return fetchHistoryItemsOldestFirst(affiliationPartitionKey(scopusAffiliationId)).stream()
        .map(item -> toHistoryEntry(item, ScopusMappingItems::toAffiliationMapping))
        .toList();
  }

  private static Stream<String> distinctIds(Collection<String> ids) {
    return ids.stream().filter(Objects::nonNull).distinct();
  }

  private static boolean isConditionalCheckFailure(TransactionCanceledException exception) {
    return exception.hasCancellationReasons()
        && exception.cancellationReasons().stream()
            .map(CancellationReason::code)
            .anyMatch(CONDITIONAL_CHECK_FAILED::equals);
  }

  private List<Map<String, AttributeValue>> fetchConsistently(
      List<Map<String, AttributeValue>> keys) {
    var keysWithConsistentRead =
        KeysAndAttributes.builder().keys(keys).consistentRead(true).build();
    return fetchRetryingUnprocessedKeys(Map.of(tableName, keysWithConsistentRead), 0);
  }

  private List<Map<String, AttributeValue>> fetchRetryingUnprocessedKeys(
      Map<String, KeysAndAttributes> requestItems, int retryCount) {
    var response =
        client.batchGetItem(BatchGetItemRequest.builder().requestItems(requestItems).build());
    var fetchedItems = response.responses().getOrDefault(tableName, emptyList());
    if (response.unprocessedKeys().isEmpty()) {
      return fetchedItems;
    }
    if (retryCount >= MAX_BATCH_GET_RETRIES) {
      throw new IllegalStateException(
          "DynamoDB left keys unprocessed after %d retries".formatted(MAX_BATCH_GET_RETRIES));
    }
    waitBeforeRetry(retryCount);
    var retriedItems = fetchRetryingUnprocessedKeys(response.unprocessedKeys(), retryCount + 1);
    return Stream.concat(fetchedItems.stream(), retriedItems.stream()).toList();
  }

  @SuppressWarnings("PMD.DoNotUseThreads")
  private void waitBeforeRetry(int retryCount) {
    var exponentialBackoff = initialBackoff.multipliedBy(1L << retryCount);
    try {
      Thread.sleep(
          exponentialBackoff.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : exponentialBackoff);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(INTERRUPTED_MESSAGE, exception);
    }
  }

  private StoredAuthorMapping fetchStoredAuthorMapping(String scopusAuthorId)
      throws ScopusMappingConflictException {
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

  private ScopusAuthorMapping newCuratorMapping(
      String scopusAuthorId,
      String cristinPersonId,
      String username,
      Optional<ScopusAuthorMapping> storedMapping,
      Optional<Integer> replacedRevision) {
    return new ScopusAuthorMapping(
        scopusAuthorId,
        cristinPersonId,
        storedMapping.map(ScopusAuthorMapping::scopusName).orElse(null),
        storedMapping.map(ScopusAuthorMapping::orcid).orElse(null),
        null,
        null,
        false,
        ScopusMappingSource.CURATOR,
        clock.instant().truncatedTo(ChronoUnit.MILLIS),
        username,
        replacedRevision.orElse(NO_REVISION) + 1,
        false);
  }

  private void writeNewVersion(
      ScopusAuthorMapping mapping,
      ScopusCorrectionContext correctionContext,
      Optional<Integer> replacedRevision)
      throws ScopusMappingConflictException {
    var historyPut = putIfItemDoesNotExist(toHistoryItem(mapping, correctionContext));
    var currentPut =
        replacedRevision
            .map(revision -> putIfRevisionMatches(toCurrentItem(mapping), revision))
            .orElseGet(() -> putIfItemDoesNotExist(toCurrentItem(mapping)));
    var request =
        TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder().put(historyPut).build(),
                TransactWriteItem.builder().put(currentPut).build())
            .build();
    try {
      client.transactWriteItems(request);
    } catch (TransactionCanceledException exception) {
      if (isConditionalCheckFailure(exception)) {
        throw new ScopusMappingConflictException(
            "Scopus author mapping %s changed while revision %d was being written"
                .formatted(mapping.scopusAuthorId(), mapping.revision()),
            exception);
      }
      throw exception;
    }
  }

  private Put putIfItemDoesNotExist(Map<String, AttributeValue> item) {
    return Put.builder()
        .tableName(tableName)
        .item(item)
        .conditionExpression(ITEM_DOES_NOT_EXIST_CONDITION)
        .expressionAttributeNames(
            Map.of(PARTITION_KEY_NAME_PLACEHOLDER, PRIMARY_KEY_PARTITION_KEY_NAME))
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
   * one id, a handful of small items, read rarely.
   */
  private List<Map<String, AttributeValue>> fetchHistoryItemsOldestFirst(String partitionKey) {
    return client
        .queryPaginator(
            QueryRequest.builder()
                .tableName(tableName)
                .consistentRead(true)
                .keyConditionExpression(HISTORY_KEY_CONDITION)
                .expressionAttributeNames(
                    Map.of(
                        PARTITION_KEY_NAME_PLACEHOLDER,
                        PRIMARY_KEY_PARTITION_KEY_NAME,
                        SORT_KEY_NAME_PLACEHOLDER,
                        PRIMARY_KEY_SORT_KEY_NAME))
                .expressionAttributeValues(
                    Map.of(
                        PARTITION_KEY_VALUE_PLACEHOLDER,
                        AttributeValue.fromS(partitionKey),
                        HISTORY_PREFIX_VALUE_PLACEHOLDER,
                        AttributeValue.fromS(HISTORY_SORT_KEY_PREFIX)))
                .build())
        .items()
        .stream()
        .sorted(Comparator.comparingInt(ScopusMappingItems::revisionOf))
        .toList();
  }
}
