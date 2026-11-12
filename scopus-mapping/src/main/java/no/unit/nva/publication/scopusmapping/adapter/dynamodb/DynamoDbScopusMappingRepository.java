package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItems.parseOrLogAndSkip;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItems.partitionKeyOf;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.HISTORY_SORT_KEY_PREFIX;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.affiliationPartitionKey;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.authorPartitionKey;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import no.unit.nva.publication.scopusmapping.domain.ScopusMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingHistoryEntry;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingRepository;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappings;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

/**
 * Keeps the mappings as items in the import candidates table. The module README describes the item
 * format. A stored item that can't be read as a mapping is logged as an error and treated as
 * absent, so one bad item does not hide the other mappings.
 */
public class DynamoDbScopusMappingRepository implements ScopusMappingRepository {

  private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(50);
  private static final String PARTITION_KEY_VALUE_PLACEHOLDER = ":partitionKey";
  private static final String HISTORY_PREFIX_VALUE_PLACEHOLDER = ":historyPrefix";
  private static final String HISTORY_KEY_CONDITION =
      "%s = %s AND begins_with(%s, %s)"
          .formatted(
              PRIMARY_KEY_PARTITION_KEY_NAME,
              PARTITION_KEY_VALUE_PLACEHOLDER,
              PRIMARY_KEY_SORT_KEY_NAME,
              HISTORY_PREFIX_VALUE_PLACEHOLDER);

  private final DynamoDbClient client;
  private final String tableName;
  private final BatchItemReader batchItemReader;
  private final AuthorMappingCorrectionStore correctionStore;

  public DynamoDbScopusMappingRepository(DynamoDbClient client, String tableName) {
    this(client, tableName, DEFAULT_INITIAL_BACKOFF);
  }

  DynamoDbScopusMappingRepository(
      DynamoDbClient client, String tableName, Duration initialBackoff) {
    this.client = client;
    this.tableName = tableName;
    this.batchItemReader = new BatchItemReader(client, tableName, initialBackoff);
    this.correctionStore = new AuthorMappingCorrectionStore(client, tableName);
  }

  @Override
  public ScopusMappings findCurrentMappings(
      Collection<ScopusAuthorIdentifier> scopusAuthorIdentifiers,
      Collection<ScopusAffiliationIdentifier> scopusAffiliationIdentifiers) {
    var keys =
        Stream.concat(
                scopusAuthorIdentifiers.stream()
                    .distinct()
                    .map(ScopusMappingKeys::authorPartitionKey),
                scopusAffiliationIdentifiers.stream()
                    .distinct()
                    .map(ScopusMappingKeys::affiliationPartitionKey))
            .map(ScopusMappingKeys::currentKey)
            .toList();
    var items = batchItemReader.read(keys);
    return ScopusMappings.of(authorMappingsOf(items), affiliationMappingsOf(items));
  }

  @Override
  public List<ScopusMappingHistoryEntry<ScopusAuthorMapping>> findAuthorMappingHistory(
      ScopusAuthorIdentifier scopusAuthorIdentifier) {
    return findHistoryOldestFirst(
        authorPartitionKey(scopusAuthorIdentifier),
        item -> ScopusAuthorMappingDao.fromItem(item).toHistoryEntry());
  }

  @Override
  public List<ScopusMappingHistoryEntry<ScopusAffiliationMapping>> findAffiliationMappingHistory(
      ScopusAffiliationIdentifier scopusAffiliationIdentifier) {
    return findHistoryOldestFirst(
        affiliationPartitionKey(scopusAffiliationIdentifier),
        item -> ScopusAffiliationMappingDao.fromItem(item).toHistoryEntry());
  }

  @Override
  public Optional<ScopusAuthorMapping> findAuthorMapping(
      ScopusAuthorIdentifier scopusAuthorIdentifier) {
    return correctionStore.findMapping(scopusAuthorIdentifier);
  }

  @Override
  public void saveCorrection(
      ScopusAuthorMapping newVersion, ScopusCorrectionContext correctionContext) {
    correctionStore.saveCorrection(newVersion, correctionContext);
  }

  /**
   * Reads consistently, so history shown right after a correction includes it. The cost is small:
   * one identifier, a handful of small items, read rarely. Orders by revision, since the sort key
   * starts with the timestamp and clocks can disagree.
   */
  private <T extends ScopusMapping> List<ScopusMappingHistoryEntry<T>> findHistoryOldestFirst(
      String partitionKey,
      Function<Map<String, AttributeValue>, ScopusMappingHistoryEntry<T>> historyEntryReader) {
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
        .flatMap(item -> parseOrLogAndSkip(item, historyEntryReader).stream())
        .sorted(Comparator.comparingInt(entry -> entry.mapping().version().revision()))
        .toList();
  }

  private static List<ScopusAuthorMapping> authorMappingsOf(
      List<Map<String, AttributeValue>> items) {
    return items.stream()
        .filter(DynamoDbScopusMappingRepository::isAuthorItem)
        .flatMap(
            item ->
                parseOrLogAndSkip(
                    item, authorItem -> ScopusAuthorMappingDao.fromItem(authorItem).toMapping())
                    .stream())
        .toList();
  }

  private static List<ScopusAffiliationMapping> affiliationMappingsOf(
      List<Map<String, AttributeValue>> items) {
    return items.stream()
        .filter(item -> !isAuthorItem(item))
        .flatMap(
            item ->
                parseOrLogAndSkip(
                    item,
                    affiliationItem ->
                        ScopusAffiliationMappingDao.fromItem(affiliationItem).toMapping())
                    .stream())
        .toList();
  }

  private static boolean isAuthorItem(Map<String, AttributeValue> item) {
    return ScopusMappingKeys.isAuthorPartitionKey(partitionKeyOf(item));
  }
}
