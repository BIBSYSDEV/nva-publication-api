package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.commons.json.JsonUtils.dynamoObjectMapper;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Turns DAO records into DynamoDB items and back. The attributes of a DAO are plain top-level
 * attributes of the item, not a JSON blob, because the PIA migration script writes the same items.
 */
final class ScopusMappingItems {

  static final String REVISION = "revision";
  private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d+");
  private static final String UNKNOWN_PARTITION_KEY = "without " + PRIMARY_KEY_PARTITION_KEY_NAME;
  private static final String UNKNOWN_SORT_KEY = "without " + PRIMARY_KEY_SORT_KEY_NAME;
  private static final Logger logger = LoggerFactory.getLogger(ScopusMappingItems.class);

  private ScopusMappingItems() {}

  static Map<String, AttributeValue> toItem(Object dao) {
    var json = attempt(() -> dynamoObjectMapper.writeValueAsString(dao)).orElseThrow();
    return EnhancedDocument.fromJson(json).toMap();
  }

  /**
   * Reads an item as a DAO. Unknown attributes are ignored, and a value stored as another DynamoDB
   * type is coerced when it can be, like in the other DAOs of this repository. The revision is the
   * exception, see {@link #revisionOf}.
   */
  static <T> T fromItem(Map<String, AttributeValue> item, Class<T> daoClass) {
    revisionOf(item);
    var json = EnhancedDocument.fromAttributeValueMap(item).toJson();
    return attempt(() -> dynamoObjectMapper.readValue(json, daoClass))
        .orElseThrow(
            failure ->
                new IllegalArgumentException(
                    "Scopus mapping item %s is malformed: %s"
                        .formatted(partitionKeyOf(item), failure.getException().getMessage()),
                    failure.getException()));
  }

  /**
   * The revision guards writes with a condition on the stored number, so it is not coerced: it must
   * be stored as a whole number.
   */
  static int revisionOf(Map<String, AttributeValue> item) {
    return Optional.ofNullable(item.get(REVISION))
        .map(AttributeValue::n)
        .filter(number -> WHOLE_NUMBER.matcher(number).matches())
        .map(Integer::valueOf)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Scopus mapping item %s has no whole-number revision"
                        .formatted(partitionKeyOf(item))));
  }

  static String partitionKeyOf(Map<String, AttributeValue> item) {
    return textOf(item, PRIMARY_KEY_PARTITION_KEY_NAME).orElse(UNKNOWN_PARTITION_KEY);
  }

  /** Parses an item, or logs it as malformed and returns empty. */
  static <T> Optional<T> parseOrLogAndSkip(
      Map<String, AttributeValue> item, Function<Map<String, AttributeValue>, T> parser) {
    return attempt(() -> parser.apply(item))
        .toOptional(
            failure ->
                logger.error(
                    "Leaving out malformed Scopus mapping item {} {}: {}",
                    partitionKeyOf(item),
                    textOf(item, PRIMARY_KEY_SORT_KEY_NAME).orElse(UNKNOWN_SORT_KEY),
                    failure.getException().getMessage()));
  }

  private static Optional<String> textOf(Map<String, AttributeValue> item, String attributeName) {
    return Optional.ofNullable(item.get(attributeName)).map(AttributeValue::s);
  }
}
