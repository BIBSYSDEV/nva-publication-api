package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;

/**
 * The keys of the stored mapping items. Everything about one Scopus identifier shares a partition
 * key, and the identifier is stored in that key, not in an attribute.
 */
final class ScopusMappingKeys {

  static final String CURRENT_SORT_KEY = "CURRENT";
  private static final String AUTHOR_PARTITION_KEY_PREFIX = "AUTHOR#SCOPUS#";
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
