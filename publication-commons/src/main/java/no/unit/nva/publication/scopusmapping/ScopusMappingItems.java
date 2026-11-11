package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.model.business.StorageModelConfig.dynamoDbObjectMapper;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * The DynamoDB item format of Scopus id mappings: the mapping's own fields as top-level attributes,
 * with the Scopus id in the partition key instead of in an attribute. The PIA migration script
 * writes the same format, so any change here must be made there too (see the golden file described
 * in the module README).
 */
final class ScopusMappingItems {

  static final String REVISION = "revision";
  private static final String AUTHOR_PARTITION_KEY_PREFIX = "AUTHOR#SCOPUS#";
  private static final String AFFILIATION_PARTITION_KEY_PREFIX = "AFFILIATION#SCOPUS#";
  private static final String CURRENT_SORT_KEY = "CURRENT";
  private static final String AUTHOR_MAPPING_TYPE = "ScopusAuthorMapping";
  private static final String TYPE = "type";
  private static final String SCOPUS_AUTHOR_ID = "scopusAuthorId";
  private static final String NEEDS_REVIEW = "needsReview";
  private static final String UPDATED_AT = "updatedAt";
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
    return toAttributeValues(authorJson(mapping, CURRENT_SORT_KEY));
  }

  private static Map<String, AttributeValue> currentKey(String partitionKey) {
    return Map.of(
        PRIMARY_KEY_PARTITION_KEY_NAME,
        AttributeValue.fromS(partitionKey),
        PRIMARY_KEY_SORT_KEY_NAME,
        AttributeValue.fromS(CURRENT_SORT_KEY));
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
}
