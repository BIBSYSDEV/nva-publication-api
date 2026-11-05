package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The DynamoDB item format of Scopus id mappings: the mapping's own fields as top-level attributes,
 * with the Scopus id in the partition key instead of in an attribute. The PIA migration script
 * writes the same format, so any change here must be made there too (see the golden file described
 * in the module README).
 */
final class ScopusMappingItems {

  static final String HISTORY_SORT_KEY_PREFIX = "HISTORY#";
  static final String REVISION = "revision";
  private static final String HISTORY_SORT_KEY_SEPARATOR = "#";
  private static final String SCOPUS_PUBLICATION_ID = "scopusPublicationId";
  private static final String CANDIDATES = "candidates";
  private static final TypeReference<List<Map<String, String>>> CANDIDATES_TYPE =
      new TypeReference<>() {};
  private static final String AUTHOR_PARTITION_KEY_PREFIX = "AUTHOR#SCOPUS#";
  private static final String AFFILIATION_PARTITION_KEY_PREFIX = "AFFILIATION#SCOPUS#";
  private static final String CURRENT_SORT_KEY = "CURRENT";
  private static final String AUTHOR_MAPPING_TYPE = "ScopusAuthorMapping";
  private static final String AFFILIATION_MAPPING_TYPE = "ScopusAffiliationMapping";
  private static final String TYPE = "type";
  private static final String SCOPUS_AUTHOR_ID = "scopusAuthorId";
  private static final String SCOPUS_AFFILIATION_ID = "scopusAffiliationId";
  private static final String NEEDS_REVIEW = "needsReview";
  private static final String UPDATED_AT = "updatedAt";
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
            .flatMap(item -> parseOrLogAndSkip(item, ScopusMappingItems::toAuthorMapping).stream())
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
    return toAttributeValues(authorJson(mapping, CURRENT_SORT_KEY));
  }

  static Map<String, AttributeValue> toHistoryItem(
      ScopusAuthorMapping mapping, ScopusCorrectionContext correctionContext) {
    var json = authorJson(mapping, historySortKey(mapping.updatedAt(), mapping.revision()));
    json.setAll(dynamoDbObjectMapper.<ObjectNode>valueToTree(correctionContext));
    return toAttributeValues(json);
  }

  /**
   * Reads a {@code HISTORY} item. The values of the PIA candidates are read as text, whether they
   * are stored as strings, numbers or booleans.
   */
  static <T> ScopusMappingHistoryEntry<T> toHistoryEntry(
      Map<String, AttributeValue> item, Function<Map<String, AttributeValue>, T> mappingReader) {
    var json = toJson(item);
    var correctionContext =
        json.has(SCOPUS_PUBLICATION_ID)
            ? dynamoDbObjectMapper.convertValue(json, ScopusCorrectionContext.class)
            : null;
    List<Map<String, String>> candidates =
        json.has(CANDIDATES)
            ? dynamoDbObjectMapper.convertValue(json.get(CANDIDATES), CANDIDATES_TYPE)
            : emptyList();
    return new ScopusMappingHistoryEntry<>(
        mappingReader.apply(item), correctionContext, candidates);
  }

  static ScopusAuthorMapping toAuthorMapping(Map<String, AttributeValue> item) {
    return toMapping(
        item,
        AUTHOR_PARTITION_KEY_PREFIX,
        AUTHOR_MAPPING_TYPE,
        SCOPUS_AUTHOR_ID,
        ScopusAuthorMapping.class);
  }

  static ScopusAffiliationMapping toAffiliationMapping(Map<String, AttributeValue> item) {
    return toMapping(
        item,
        AFFILIATION_PARTITION_KEY_PREFIX,
        AFFILIATION_MAPPING_TYPE,
        SCOPUS_AFFILIATION_ID,
        ScopusAffiliationMapping.class);
  }

  /**
   * The revision guards writes with a condition on the stored number, so unlike the other
   * attributes it is not coerced: it must be stored as a whole number.
   */
  static int revisionOf(Map<String, AttributeValue> item) {
    return revisionOf(toJson(item));
  }

  /** Parses an item, or logs it as malformed and returns empty. */
  static <T> Optional<T> parseOrLogAndSkip(
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

  private static boolean hasAuthorPartitionKey(Map<String, AttributeValue> item) {
    return partitionKeyOf(item).startsWith(AUTHOR_PARTITION_KEY_PREFIX);
  }

  private static String partitionKeyOf(Map<String, AttributeValue> item) {
    return Optional.ofNullable(item.get(PRIMARY_KEY_PARTITION_KEY_NAME))
        .map(AttributeValue::s)
        .orElse(UNKNOWN_PARTITION_KEY);
  }

  private static <T> T toMapping(
      Map<String, AttributeValue> item,
      String partitionKeyPrefix,
      String expectedType,
      String scopusIdName,
      Class<T> mappingClass) {
    var json = toJson(item);
    requireType(json, expectedType);
    revisionOf(json);
    json.put(scopusIdName, idFromPartitionKey(json, partitionKeyPrefix));
    return attempt(() -> dynamoDbObjectMapper.treeToValue(json, mappingClass))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    "Scopus mapping item %s is malformed: %s"
                        .formatted(partitionKeyOf(json), failure.getException().getMessage()),
                    failure.getException()));
  }

  private static int revisionOf(ObjectNode json) {
    var revision = json.path(REVISION);
    if (!revision.isIntegralNumber()) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s has no whole-number revision".formatted(partitionKeyOf(json)));
    }
    return revision.intValue();
  }

  private static void requireType(ObjectNode json, String expectedType) {
    var type = json.path(TYPE).asText();
    if (!expectedType.equals(type)) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s has type %s, not %s"
              .formatted(partitionKeyOf(json), type, expectedType));
    }
  }

  private static String idFromPartitionKey(ObjectNode json, String prefix) {
    var partitionKey = partitionKeyOf(json);
    if (!partitionKey.startsWith(prefix)) {
      throw new IllegalArgumentException(
          "Scopus mapping item %s does not start with %s".formatted(partitionKey, prefix));
    }
    return partitionKey.substring(prefix.length());
  }

  private static String partitionKeyOf(ObjectNode json) {
    return json.path(PRIMARY_KEY_PARTITION_KEY_NAME).asText(UNKNOWN_PARTITION_KEY);
  }

  private static Map<String, AttributeValue> currentKey(String partitionKey) {
    return Map.of(
        PRIMARY_KEY_PARTITION_KEY_NAME,
        AttributeValue.fromS(partitionKey),
        PRIMARY_KEY_SORT_KEY_NAME,
        AttributeValue.fromS(CURRENT_SORT_KEY));
  }

  private static String historySortKey(Instant updatedAt, int revision) {
    return HISTORY_SORT_KEY_PREFIX
        + TIMESTAMP_FORMAT.format(updatedAt)
        + HISTORY_SORT_KEY_SEPARATOR
        + revision;
  }

  private static ObjectNode authorJson(ScopusAuthorMapping mapping, String sortKey) {
    var json = dynamoDbObjectMapper.<ObjectNode>valueToTree(mapping);
    json.remove(SCOPUS_AUTHOR_ID);
    json.put(PRIMARY_KEY_PARTITION_KEY_NAME, authorPartitionKey(mapping.scopusAuthorId()));
    json.put(PRIMARY_KEY_SORT_KEY_NAME, sortKey);
    json.put(TYPE, AUTHOR_MAPPING_TYPE);
    json.put(UPDATED_AT, TIMESTAMP_FORMAT.format(mapping.updatedAt()));
    removeIfFalse(json, NEEDS_REVIEW);
    removeIfFalse(json, DELETED);
    return json;
  }

  private static void removeIfFalse(ObjectNode json, String flagName) {
    if (!json.path(flagName).asBoolean()) {
      json.remove(flagName);
    }
  }

  private static Map<String, AttributeValue> toAttributeValues(ObjectNode json) {
    return EnhancedDocument.fromJson(json.toString()).toMap();
  }

  private static ObjectNode toJson(Map<String, AttributeValue> item) {
    var json = EnhancedDocument.fromAttributeValueMap(item).toJson();
    return attempt(() -> (ObjectNode) dynamoDbObjectMapper.readTree(json)).orElseThrow();
  }
}
