package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItems.REVISION;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItems.partitionKeyOf;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.authorPartitionKey;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.currentKey;
import static no.unit.nva.publication.scopusmapping.domain.ScopusMappingVersion.FIRST_REVISION;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static nva.commons.core.attempt.Try.attempt;

import java.util.Map;
import java.util.Optional;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

/** Reads the stored mapping a correction starts from, and saves the correction. */
final class AuthorMappingCorrectionStore {

  private static final String REVISION_NAME_PLACEHOLDER = "#revision";
  private static final String EXPECTED_REVISION_VALUE_PLACEHOLDER = ":expectedRevision";
  private static final String ITEM_DOES_NOT_EXIST_CONDITION =
      "attribute_not_exists(" + PRIMARY_KEY_PARTITION_KEY_NAME + ")";
  private static final String REVISION_MATCHES_CONDITION =
      REVISION_NAME_PLACEHOLDER + " = " + EXPECTED_REVISION_VALUE_PLACEHOLDER;

  private final DynamoDbClient client;
  private final String tableName;

  AuthorMappingCorrectionStore(DynamoDbClient client, String tableName) {
    this.client = client;
    this.tableName = tableName;
  }

  /**
   * Reads consistently, so the correction starts from what is stored now. A stored item that can't
   * be read as a mapping is a fault in the data, and fails loudly so it gets repaired.
   */
  Optional<ScopusAuthorMapping> findMapping(ScopusAuthorIdentifier scopusAuthorIdentifier) {
    var response =
        client.getItem(
            GetItemRequest.builder()
                .tableName(tableName)
                .key(currentKey(authorPartitionKey(scopusAuthorIdentifier)))
                .consistentRead(true)
                .build());
    return response.hasItem() ? Optional.of(readableMappingOf(response.item())) : Optional.empty();
  }

  /**
   * Writes a new {@code HISTORY} item and replaces {@code CURRENT} in one transaction. The first
   * version requires that no {@code CURRENT} item exists, and a later version that the stored
   * revision is the one before it.
   */
  void saveCorrection(ScopusAuthorMapping newVersion, ScopusCorrectionContext correctionContext) {
    var historyItem = ScopusAuthorMappingDao.history(newVersion, correctionContext).toItem();
    var currentItem = ScopusAuthorMappingDao.current(newVersion).toItem();
    var newRevision = newVersion.version().revision();
    var currentPut =
        newRevision == FIRST_REVISION
            ? putIfItemDoesNotExist(currentItem)
            : putIfRevisionMatches(currentItem, newRevision - 1);
    client.transactWriteItems(
        TransactWriteItemsRequest.builder()
            .transactItems(
                TransactWriteItem.builder().put(putIfItemDoesNotExist(historyItem)).build(),
                TransactWriteItem.builder().put(currentPut).build())
            .build());
  }

  private static ScopusAuthorMapping readableMappingOf(Map<String, AttributeValue> item) {
    return attempt(() -> ScopusAuthorMappingDao.fromItem(item).toMapping())
        .orElseThrow(
            failure ->
                new IllegalStateException(
                    "Stored Scopus author mapping %s can't be read and has to be repaired: %s"
                        .formatted(partitionKeyOf(item), failure.getException().getMessage()),
                    failure.getException()));
  }

  private Put putIfItemDoesNotExist(Map<String, AttributeValue> item) {
    return Put.builder()
        .tableName(tableName)
        .item(item)
        .conditionExpression(ITEM_DOES_NOT_EXIST_CONDITION)
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
}
