package no.unit.nva.publication.scopusmapping;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static final String AFFILIATION_MAPPING_TYPE = "ScopusAffiliationMapping";
  private static final String TYPE = "type";
  private static final String CRISTIN_PERSON_ID = "cristinPersonId";
  private static final String CRISTIN_INSTITUTION_ID = "cristinInstitutionId";
  private static final String CRISTIN_UNIT_ID = "cristinUnitId";
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
  private static final String UNKNOWN_PARTITION_KEY = "without " + PRIMARY_KEY_PARTITION_KEY_NAME;
  private static final Logger logger = LoggerFactory.getLogger(ScopusMappingItems.class);

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

  /**
   * Turns the {@code CURRENT} items of a batch read into mappings, leaving out deleted ones. The
   * key decides whether an item is an author or an affiliation mapping. A malformed item is logged
   * and left out, so one bad item does not hide the other mappings of a Scopus record.
   */
  static ScopusMappings toCurrentMappings(List<Map<String, AttributeValue>> currentItems) {
    var authorMappings =
        currentItems.stream()
            .filter(ScopusMappingItems::hasAuthorPartitionKey)
            .flatMap(item -> toAuthorMappingOrLog(item).stream())
            .filter(mapping -> !mapping.deleted())
            .collect(toMap(ScopusAuthorMapping::scopusAuthorId, identity()));
    var affiliationMappings =
        currentItems.stream()
            .filter(item -> !hasAuthorPartitionKey(item))
            .flatMap(
                item -> parseOrLogAndSkip(item, ScopusMappingItems::toAffiliationMapping).stream())
            .filter(mapping -> !mapping.deleted())
            .collect(toMap(ScopusAffiliationMapping::scopusAffiliationId, identity()));
    return new ScopusMappings(authorMappings, affiliationMappings);
  }

  static Map<String, AttributeValue> toCurrentItem(ScopusAuthorMapping mapping) {
    var item = authorAttributes(mapping);
    item.put(PRIMARY_KEY_SORT_KEY_NAME, AttributeValue.fromS(CURRENT_SORT_KEY));
    return item;
  }

  static ScopusAuthorMapping toAuthorMapping(Map<String, AttributeValue> item) {
    requireType(item, AUTHOR_MAPPING_TYPE);
    return new ScopusAuthorMapping(
        idFromPartitionKey(item, AUTHOR_PARTITION_KEY_PREFIX),
        requiredString(item, CRISTIN_PERSON_ID),
        optionalString(item, SCOPUS_NAME),
        optionalString(item, ORCID),
        optionalInteger(item, MAPPING_COUNT),
        optionalInteger(item, TOTAL_MAPPING_COUNT),
        isTrue(item, NEEDS_REVIEW),
        ScopusMappingSource.valueOf(requiredString(item, SOURCE)),
        Instant.parse(requiredString(item, UPDATED_AT)),
        requiredString(item, UPDATED_BY),
        revisionOf(item),
        isTrue(item, DELETED));
  }

  static ScopusAffiliationMapping toAffiliationMapping(Map<String, AttributeValue> item) {
    requireType(item, AFFILIATION_MAPPING_TYPE);
    return new ScopusAffiliationMapping(
        idFromPartitionKey(item, AFFILIATION_PARTITION_KEY_PREFIX),
        requiredString(item, CRISTIN_INSTITUTION_ID),
        requiredString(item, CRISTIN_UNIT_ID),
        optionalString(item, SCOPUS_NAME),
        optionalInteger(item, MAPPING_COUNT),
        optionalInteger(item, TOTAL_MAPPING_COUNT),
        isTrue(item, NEEDS_REVIEW),
        ScopusMappingSource.valueOf(requiredString(item, SOURCE)),
        Instant.parse(requiredString(item, UPDATED_AT)),
        requiredString(item, UPDATED_BY),
        revisionOf(item),
        isTrue(item, DELETED));
  }

  /** Parses an author item, or logs it as malformed and returns empty. */
  static Optional<ScopusAuthorMapping> toAuthorMappingOrLog(Map<String, AttributeValue> item) {
    return parseOrLogAndSkip(item, ScopusMappingItems::toAuthorMapping);
  }

  static int revisionOf(Map<String, AttributeValue> item) {
    return requiredInteger(item, REVISION);
  }

  private static boolean hasAuthorPartitionKey(Map<String, AttributeValue> item) {
    return partitionKeyOf(item).startsWith(AUTHOR_PARTITION_KEY_PREFIX);
  }

  private static <T> Optional<T> parseOrLogAndSkip(
      Map<String, AttributeValue> item, Function<Map<String, AttributeValue>, T> parser) {
    return attempt(() -> parser.apply(item))
        .toOptional(failure -> logMalformedItem(item, failure.getException()));
  }

  private static void logMalformedItem(Map<String, AttributeValue> item, Exception exception) {
    logger.error(
        "Treating malformed Scopus mapping item {} as no mapping: {}",
        partitionKeyOf(item),
        exception.getMessage());
  }

  private static void requireType(Map<String, AttributeValue> item, String expectedType) {
    var type = requiredString(item, TYPE);
    if (!expectedType.equals(type)) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s has type %s, not %s"
              .formatted(partitionKeyOf(item), type, expectedType));
    }
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

  private static String idFromPartitionKey(Map<String, AttributeValue> item, String prefix) {
    var partitionKey = requiredString(item, PRIMARY_KEY_PARTITION_KEY_NAME);
    if (!partitionKey.startsWith(prefix)) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s does not start with %s".formatted(partitionKey, prefix));
    }
    return partitionKey.substring(prefix.length());
  }

  private static boolean isTrue(Map<String, AttributeValue> item, String attributeName) {
    return attributeOfType(item, attributeName, AttributeValue.Type.BOOL)
        .map(AttributeValue::bool)
        .orElse(false);
  }

  private static Integer optionalInteger(Map<String, AttributeValue> item, String attributeName) {
    return attributeOfType(item, attributeName, AttributeValue.Type.N)
        .map(attribute -> parseInteger(item, attributeName, attribute.n()))
        .orElse(null);
  }

  private static int requiredInteger(Map<String, AttributeValue> item, String attributeName) {
    return parseInteger(
        item,
        attributeName,
        requiredAttributeOfType(item, attributeName, AttributeValue.Type.N).n());
  }

  private static String optionalString(Map<String, AttributeValue> item, String attributeName) {
    return attributeOfType(item, attributeName, AttributeValue.Type.S)
        .map(AttributeValue::s)
        .orElse(null);
  }

  private static String requiredString(Map<String, AttributeValue> item, String attributeName) {
    return requiredAttributeOfType(item, attributeName, AttributeValue.Type.S).s();
  }

  private static AttributeValue requiredAttributeOfType(
      Map<String, AttributeValue> item, String attributeName, AttributeValue.Type expectedType) {
    return attributeOfType(item, attributeName, expectedType)
        .orElseThrow(() -> missingAttribute(item, attributeName));
  }

  private static Optional<AttributeValue> attributeOfType(
      Map<String, AttributeValue> item, String attributeName, AttributeValue.Type expectedType) {
    var attribute = item.get(attributeName);
    if (isNull(attribute)) {
      return Optional.empty();
    }
    if (attribute.type() != expectedType) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s has %s of type %s, not %s"
              .formatted(partitionKeyOf(item), attributeName, attribute.type(), expectedType));
    }
    return Optional.of(attribute);
  }

  private static int parseInteger(
      Map<String, AttributeValue> item, String attributeName, String number) {
    try {
      return Integer.parseInt(number);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s has %s %s, which is not an integer"
              .formatted(partitionKeyOf(item), attributeName, number),
          exception);
    }
  }

  private static IllegalArgumentException missingAttribute(
      Map<String, AttributeValue> item, String attributeName) {
    return new IllegalArgumentException(
        "Scopus mapping item %s has no %s".formatted(partitionKeyOf(item), attributeName));
  }

  private static String partitionKeyOf(Map<String, AttributeValue> item) {
    return Optional.ofNullable(item.get(PRIMARY_KEY_PARTITION_KEY_NAME))
        .map(AttributeValue::s)
        .orElse(UNKNOWN_PARTITION_KEY);
  }
}
