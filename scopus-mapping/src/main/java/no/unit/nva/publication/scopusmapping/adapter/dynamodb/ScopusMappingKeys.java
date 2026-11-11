package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Map;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingVersion;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The keys of the stored mapping items. Everything about one Scopus identifier shares a partition
 * key, and the identifier is stored in that key, not in an attribute.
 */
final class ScopusMappingKeys {

  static final String CURRENT_SORT_KEY = "CURRENT";
  static final String HISTORY_SORT_KEY_PREFIX = "HISTORY#";
  private static final String HISTORY_SORT_KEY_SEPARATOR = "#";
  private static final String AUTHOR_PARTITION_KEY_PREFIX = "AUTHOR#SCOPUS#";
  private static final String AFFILIATION_PARTITION_KEY_PREFIX = "AFFILIATION#SCOPUS#";
  private static final int MILLISECOND_DIGITS = 3;
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      new DateTimeFormatterBuilder().appendInstant(MILLISECOND_DIGITS).toFormatter();

  private ScopusMappingKeys() {}

  static String authorPartitionKey(ScopusAuthorIdentifier scopusAuthorIdentifier) {
    return AUTHOR_PARTITION_KEY_PREFIX + scopusAuthorIdentifier.value();
  }

  static ScopusAuthorIdentifier authorIdentifierOf(String partitionKey) {
    return new ScopusAuthorIdentifier(withoutPrefix(partitionKey, AUTHOR_PARTITION_KEY_PREFIX));
  }

  static String affiliationPartitionKey(ScopusAffiliationIdentifier scopusAffiliationIdentifier) {
    return AFFILIATION_PARTITION_KEY_PREFIX + scopusAffiliationIdentifier.value();
  }

  static ScopusAffiliationIdentifier affiliationIdentifierOf(String partitionKey) {
    return new ScopusAffiliationIdentifier(
        withoutPrefix(partitionKey, AFFILIATION_PARTITION_KEY_PREFIX));
  }

  static boolean isAuthorPartitionKey(String partitionKey) {
    return partitionKey.startsWith(AUTHOR_PARTITION_KEY_PREFIX);
  }

  static Map<String, AttributeValue> currentKey(String partitionKey) {
    return Map.of(
        PRIMARY_KEY_PARTITION_KEY_NAME,
        AttributeValue.fromS(partitionKey),
        PRIMARY_KEY_SORT_KEY_NAME,
        AttributeValue.fromS(CURRENT_SORT_KEY));
  }

  /** Each written version is kept under {@code HISTORY#{updatedAt}#{revision}}. */
  static String historySortKey(ScopusMappingVersion version) {
    return HISTORY_SORT_KEY_PREFIX
        + timestamp(version.updatedAt())
        + HISTORY_SORT_KEY_SEPARATOR
        + version.revision();
  }

  /** Timestamps are stored with milliseconds, so keys built from them have a fixed width. */
  static String timestamp(Instant instant) {
    return TIMESTAMP_FORMAT.format(instant);
  }

  private static String withoutPrefix(String partitionKey, String prefix) {
    if (!partitionKey.startsWith(prefix)) {
      throw new IllegalArgumentException(
          "Partition key %s does not start with %s".formatted(partitionKey, prefix));
    }
    return partitionKey.substring(prefix.length());
  }
}
