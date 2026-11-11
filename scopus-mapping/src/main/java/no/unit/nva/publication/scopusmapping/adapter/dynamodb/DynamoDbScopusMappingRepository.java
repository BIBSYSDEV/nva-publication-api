package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItems.parseOrLogAndSkip;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItems.partitionKeyOf;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingRepository;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappings;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Keeps the mappings as items in the import candidates table. The module README describes the item
 * format. A stored item that can't be read as a mapping is logged as an error and treated as
 * absent, so one bad item does not hide the other mappings.
 */
public class DynamoDbScopusMappingRepository implements ScopusMappingRepository {

  private static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(50);

  private final BatchItemReader batchItemReader;

  public DynamoDbScopusMappingRepository(DynamoDbClient client, String tableName) {
    this(client, tableName, DEFAULT_INITIAL_BACKOFF);
  }

  DynamoDbScopusMappingRepository(
      DynamoDbClient client, String tableName, Duration initialBackoff) {
    this.batchItemReader = new BatchItemReader(client, tableName, initialBackoff);
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
