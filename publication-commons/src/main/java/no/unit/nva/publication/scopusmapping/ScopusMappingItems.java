package no.unit.nva.publication.scopusmapping;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The DynamoDB item format of Scopus id mappings. The PIA migration script writes the same format,
 * so any change here must be made there too (see the golden file described in the module README).
 */
final class ScopusMappingItems {

  static final String REVISION = "revision";
  private static final String AUTHOR_PARTITION_KEY_PREFIX = "AUTHOR#SCOPUS#";
  private static final String AFFILIATION_PARTITION_KEY_PREFIX = "AFFILIATION#SCOPUS#";
  private static final String CURRENT_SORT_KEY = "CURRENT";
  private static final String AUTHOR_MAPPING_TYPE = "ScopusAuthorMapping";
  private static final String TYPE = "type";
  private static final String CRISTIN_PERSON_ID = "cristinPersonId";
  private static final String SCOPUS_NAME = "scopusName";
  private static final String ORCID = "orcid";
  private static final String MAPPING_COUNT = "mappingCount";
  private static final String TOTAL_MAPPING_COUNT = "totalMappingCount";
  private static final String NEEDS_REVIEW = "needsReview";
  private static final String SOURCE = "source";
  private static final String UPDATED_AT = "updatedAt";
  private static final String UPDATED_BY = "updatedBy";
  private static final String DELETED = "deleted";
  private static final int MILLISECOND_DIGITS = 3;
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      new DateTimeFormatterBuilder().appendInstant(MILLISECOND_DIGITS).toFormatter();

  private ScopusMappingItems() {}

  static String authorPartitionKey(String scopusAuthorId) {
    return AUTHOR_PARTITION_KEY_PREFIX + scopusAuthorId;
  }

  static String affiliationPartitionKey(String scopusAffiliationId) {
    return AFFILIATION_PARTITION_KEY_PREFIX + scopusAffiliationId;
  }

  static Map<String, AttributeValue> authorCurrentKey(String scopusAuthorId) {
    return currentKey(authorPartitionKey(scopusAuthorId));
  }

  static Map<String, AttributeValue> affiliationCurrentKey(String scopusAffiliationId) {
    return currentKey(affiliationPartitionKey(scopusAffiliationId));
  }

  static Map<String, AttributeValue> toCurrentItem(ScopusAuthorMapping mapping) {
    var item = authorAttributes(mapping);
    item.put(PRIMARY_KEY_SORT_KEY_NAME, AttributeValue.fromS(CURRENT_SORT_KEY));
    return item;
  }

  private static Map<String, AttributeValue> currentKey(String partitionKey) {
    return Map.of(
        PRIMARY_KEY_PARTITION_KEY_NAME,
        AttributeValue.fromS(partitionKey),
        PRIMARY_KEY_SORT_KEY_NAME,
        AttributeValue.fromS(CURRENT_SORT_KEY));
  }

  private static Map<String, AttributeValue> authorAttributes(ScopusAuthorMapping mapping) {
    var item = new HashMap<String, AttributeValue>();
    item.put(
        PRIMARY_KEY_PARTITION_KEY_NAME,
        AttributeValue.fromS(authorPartitionKey(mapping.scopusAuthorId())));
    item.put(TYPE, AttributeValue.fromS(AUTHOR_MAPPING_TYPE));
    item.put(CRISTIN_PERSON_ID, AttributeValue.fromS(mapping.cristinPersonId()));
    putStringIfPresent(item, SCOPUS_NAME, mapping.scopusName());
    putStringIfPresent(item, ORCID, mapping.orcid());
    putNumberIfPresent(item, MAPPING_COUNT, mapping.mappingCount());
    putNumberIfPresent(item, TOTAL_MAPPING_COUNT, mapping.totalMappingCount());
    putIfTrue(item, NEEDS_REVIEW, mapping.needsReview());
    item.put(SOURCE, AttributeValue.fromS(mapping.source().name()));
    item.put(UPDATED_AT, AttributeValue.fromS(TIMESTAMP_FORMAT.format(mapping.updatedAt())));
    item.put(UPDATED_BY, AttributeValue.fromS(mapping.updatedBy()));
    item.put(REVISION, number(mapping.revision()));
    putIfTrue(item, DELETED, mapping.deleted());
    return item;
  }

  private static void putStringIfPresent(
      Map<String, AttributeValue> item, String attributeName, String value) {
    if (nonNull(value)) {
      item.put(attributeName, AttributeValue.fromS(value));
    }
  }

  private static void putNumberIfPresent(
      Map<String, AttributeValue> item, String attributeName, Integer value) {
    if (nonNull(value)) {
      item.put(attributeName, number(value));
    }
  }

  private static void putIfTrue(
      Map<String, AttributeValue> item, String attributeName, boolean value) {
    if (value) {
      item.put(attributeName, AttributeValue.fromBool(true));
    }
  }

  private static AttributeValue number(int value) {
    return AttributeValue.fromN(String.valueOf(value));
  }
}
