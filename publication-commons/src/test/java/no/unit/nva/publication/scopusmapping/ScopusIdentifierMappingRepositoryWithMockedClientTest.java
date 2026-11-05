package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusIdentifierMappingRepository.MAX_BATCH_GET_RETRIES;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.CURATOR_USERNAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.TABLE_NAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.curatorAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.authorCurrentKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Covers what the embedded table can't produce, and checks that bad arguments fail before any call
 * to DynamoDB.
 */
class ScopusIdentifierMappingRepositoryWithMockedClientTest {

  private static final String FIRST_AUTHOR = "99999990301";
  private static final String SECOND_AUTHOR = "99999990302";
  private static final String CRISTIN_PERSON_ID = "1001";
  private static final ScopusCorrectionContext CORRECTION_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000020", 1);

  private DynamoDbClient client;
  private ScopusIdentifierMappingRepository repository;

  @BeforeEach
  void setUp() {
    client = mock(DynamoDbClient.class);
    repository =
        new ScopusIdentifierMappingRepository(client, TABLE_NAME, Clock.systemUTC(), Duration.ZERO);
  }

  @Test
  void shouldRetryUnprocessedKeysUntilEveryKeyIsRead() {
    when(client.batchGetItem(any(BatchGetItemRequest.class)))
        .thenReturn(batchGetResponse(List.of(currentItem(FIRST_AUTHOR)), SECOND_AUTHOR))
        .thenReturn(batchGetResponse(List.of(currentItem(SECOND_AUTHOR))));

    var mappings =
        repository.fetchCurrentMappings(List.of(FIRST_AUTHOR, SECOND_AUTHOR), emptyList());

    assertThat(mappings.authorMappings()).containsOnlyKeys(FIRST_AUTHOR, SECOND_AUTHOR);
    verify(client, times(2)).batchGetItem(any(BatchGetItemRequest.class));
  }

  @Test
  void shouldReadWithConsistentReads() {
    when(client.batchGetItem(any(BatchGetItemRequest.class)))
        .thenReturn(batchGetResponse(List.of(currentItem(FIRST_AUTHOR))));

    repository.fetchCurrentMappings(List.of(FIRST_AUTHOR), emptyList());

    var requestCaptor = ArgumentCaptor.forClass(BatchGetItemRequest.class);
    verify(client).batchGetItem(requestCaptor.capture());
    assertThat(requestCaptor.getValue().requestItems().get(TABLE_NAME).consistentRead()).isTrue();
  }

  @Test
  void shouldFailWhenKeysStayUnprocessed() {
    when(client.batchGetItem(any(BatchGetItemRequest.class)))
        .thenReturn(batchGetResponse(emptyList(), FIRST_AUTHOR));

    assertThatThrownBy(() -> repository.fetchCurrentMappings(List.of(FIRST_AUTHOR), emptyList()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unprocessed");
    verify(client, times(MAX_BATCH_GET_RETRIES + 1)).batchGetItem(any(BatchGetItemRequest.class));
  }

  @Test
  void shouldRejectNullIdCollectionsBeforeCallingDynamoDb() {
    assertThatNullPointerException()
        .isThrownBy(() -> repository.fetchCurrentMappings(null, emptyList()))
        .withMessage("scopusAuthorIds");
    assertThatNullPointerException()
        .isThrownBy(() -> repository.fetchCurrentMappings(emptyList(), null))
        .withMessage("scopusAffiliationIds");
    verifyNoInteractions(client);
  }

  @Test
  void shouldRejectNullIdsBeforeCallingDynamoDb() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> repository.fetchCurrentMappings(Arrays.asList(FIRST_AUTHOR, null), emptyList()))
        .withMessageContaining("scopusAuthorIds");
    assertThatNullPointerException()
        .isThrownBy(
            () -> repository.fetchCurrentMappings(emptyList(), Arrays.asList("99990301", null)))
        .withMessageContaining("scopusAffiliationIds");
    verifyNoInteractions(client);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ConditionalCheckFailed", "TransactionConflict"})
  void shouldThrowConflictWhenMappingChangesBetweenReadAndWrite(String cancellationCode) {
    when(client.getItem(any(GetItemRequest.class)))
        .thenReturn(GetItemResponse.builder().item(currentItem(FIRST_AUTHOR)).build());
    var concurrentChange = transactionCanceled(cancellationCode);
    when(client.transactWriteItems(any(TransactWriteItemsRequest.class)))
        .thenThrow(concurrentChange);

    assertThatThrownBy(
            () ->
                repository.saveCristinPersonId(
                    FIRST_AUTHOR, "1002", CORRECTION_CONTEXT, CURATOR_USERNAME, Optional.of(1)))
        .isInstanceOf(ScopusMappingConflictException.class)
        .hasMessageContaining(FIRST_AUTHOR)
        .hasCause(concurrentChange);
  }

  @Test
  void shouldRethrowTransactionCancelledForAnotherReasonThanConcurrentChange() {
    when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
    var transactionConflict = transactionCanceled("ThrottlingError");
    when(client.transactWriteItems(any(TransactWriteItemsRequest.class)))
        .thenThrow(transactionConflict);

    assertThatThrownBy(
            () ->
                repository.saveCristinPersonId(
                    FIRST_AUTHOR,
                    CRISTIN_PERSON_ID,
                    CORRECTION_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.empty()))
        .isSameAs(transactionConflict);
  }

  @Test
  void shouldRejectCristinPersonUriBeforeCallingDynamoDb() {
    var cristinPersonUri = "https://api.nva.unit.no/cristin/person/" + CRISTIN_PERSON_ID;

    assertThatThrownBy(
            () ->
                repository.saveCristinPersonId(
                    FIRST_AUTHOR,
                    cristinPersonUri,
                    CORRECTION_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.empty()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cristinPersonId");
    verifyNoInteractions(client);
  }

  @Test
  void shouldRejectNullArgumentsBeforeCallingDynamoDb() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                repository.saveCristinPersonId(
                    null,
                    CRISTIN_PERSON_ID,
                    CORRECTION_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.empty()))
        .withMessage("scopusAuthorId");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                repository.saveCristinPersonId(
                    FIRST_AUTHOR, null, CORRECTION_CONTEXT, CURATOR_USERNAME, Optional.empty()))
        .withMessage("cristinPersonId");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                repository.saveCristinPersonId(
                    FIRST_AUTHOR, CRISTIN_PERSON_ID, null, CURATOR_USERNAME, Optional.empty()))
        .withMessage("correctionContext");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                repository.saveCristinPersonId(
                    FIRST_AUTHOR, CRISTIN_PERSON_ID, CORRECTION_CONTEXT, null, Optional.empty()))
        .withMessage("username");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                repository.saveCristinPersonId(
                    FIRST_AUTHOR, CRISTIN_PERSON_ID, CORRECTION_CONTEXT, CURATOR_USERNAME, null))
        .withMessage("expectedRevision");
    verifyNoInteractions(client);
  }

  private static TransactionCanceledException transactionCanceled(String cancellationCode) {
    return TransactionCanceledException.builder()
        .message("Transaction cancelled")
        .cancellationReasons(
            CancellationReason.builder().code("None").build(),
            CancellationReason.builder().code(cancellationCode).build())
        .build();
  }

  private static Map<String, AttributeValue> currentItem(String scopusAuthorId) {
    return toCurrentItem(
        curatorAuthorMapping(
            scopusAuthorId,
            CRISTIN_PERSON_ID,
            Instant.parse("2026-11-04T12:00:00.000Z"),
            1,
            false));
  }

  private static BatchGetItemResponse batchGetResponse(List<Map<String, AttributeValue>> items) {
    return BatchGetItemResponse.builder().responses(Map.of(TABLE_NAME, items)).build();
  }

  private static BatchGetItemResponse batchGetResponse(
      List<Map<String, AttributeValue>> items, String unprocessedScopusAuthorId) {
    var unprocessedKeys =
        KeysAndAttributes.builder()
            .keys(List.of(authorCurrentKey(unprocessedScopusAuthorId)))
            .consistentRead(true)
            .build();
    return BatchGetItemResponse.builder()
        .responses(Map.of(TABLE_NAME, items))
        .unprocessedKeys(Map.of(TABLE_NAME, unprocessedKeys))
        .build();
  }
}
