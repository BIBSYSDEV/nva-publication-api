package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.CURATOR_USERNAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.migratedAuthorItem;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingLocalTest.TABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Optional;
import no.unit.nva.publication.scopusmapping.application.ScopusMappingService;
import no.unit.nva.publication.scopusmapping.domain.CristinPersonIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

/**
 * Checks how a correction talks to DynamoDB, and covers the races between the read and the write,
 * which the local table can't show.
 */
class CorrectAuthorMappingWithMockedClientTest {

  private static final CristinPersonIdentifier CRISTIN_PERSON = new CristinPersonIdentifier("1002");
  private static final ScopusCorrectionContext CORRECTION_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000020", 1);

  private DynamoDbClient client;
  private ScopusMappingService service;

  @BeforeEach
  void setUp() {
    client = mock(DynamoDbClient.class);
    service =
        new ScopusMappingService(
            new DynamoDbScopusMappingRepository(client, TABLE_NAME), Clock.systemUTC());
  }

  @Test
  void shouldReadTheStoredMappingWithConsistentRead() throws ScopusMappingConflictException {
    when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());

    service.correctAuthorMapping(
        SCOPUS_AUTHOR, CRISTIN_PERSON, CORRECTION_CONTEXT, CURATOR_USERNAME, Optional.empty());

    var requestCaptor = ArgumentCaptor.forClass(GetItemRequest.class);
    verify(client).getItem(requestCaptor.capture());
    assertThat(requestCaptor.getValue().consistentRead()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"ConditionalCheckFailed", "TransactionConflict"})
  void shouldThrowConflictWhenMappingChangesBetweenReadAndWrite(String cancellationCode) {
    when(client.getItem(any(GetItemRequest.class)))
        .thenReturn(GetItemResponse.builder().item(migratedAuthorItem()).build());
    var concurrentChange = transactionCanceled(cancellationCode);
    when(client.transactWriteItems(any(TransactWriteItemsRequest.class)))
        .thenThrow(concurrentChange);

    assertThatThrownBy(
            () ->
                service.correctAuthorMapping(
                    SCOPUS_AUTHOR,
                    CRISTIN_PERSON,
                    CORRECTION_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.of(1)))
        .isInstanceOf(ScopusMappingConflictException.class)
        .hasMessageContaining(SCOPUS_AUTHOR.value())
        .hasCause(concurrentChange);
  }

  @Test
  void shouldRethrowTransactionCancelledForAnotherReasonThanConcurrentChange() {
    when(client.getItem(any(GetItemRequest.class))).thenReturn(GetItemResponse.builder().build());
    var throttled = transactionCanceled("ThrottlingError");
    when(client.transactWriteItems(any(TransactWriteItemsRequest.class))).thenThrow(throttled);

    assertThatThrownBy(
            () ->
                service.correctAuthorMapping(
                    SCOPUS_AUTHOR,
                    CRISTIN_PERSON,
                    CORRECTION_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.empty()))
        .isSameAs(throttled);
  }

  private static TransactionCanceledException transactionCanceled(String cancellationCode) {
    return TransactionCanceledException.builder()
        .message("Transaction cancelled")
        .cancellationReasons(
            CancellationReason.builder().code("None").build(),
            CancellationReason.builder().code(cancellationCode).build())
        .build();
  }
}
