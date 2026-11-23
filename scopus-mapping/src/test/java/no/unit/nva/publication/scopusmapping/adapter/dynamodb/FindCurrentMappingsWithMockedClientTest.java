package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AFFILIATION;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.LocalImportCandidatesTable.DEFAULT_TABLE_NAME;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.authorPartitionKey;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingKeys.currentKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import no.unit.nva.publication.scopusmapping.application.ScopusMappingService;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.BatchGetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

/**
 * Covers what the local table can't produce, and checks that bad arguments fail before any call to
 * DynamoDB.
 */
class FindCurrentMappingsWithMockedClientTest {

  private static final ScopusAuthorIdentifier FIRST_AUTHOR =
      new ScopusAuthorIdentifier("99999990301");
  private static final ScopusAuthorIdentifier SECOND_AUTHOR =
      new ScopusAuthorIdentifier("99999990302");

  private DynamoDbClient client;
  private ScopusMappingService service;

  @BeforeEach
  void setUp() {
    client = mock(DynamoDbClient.class);
    when(client.batchGetItemPaginator(any(BatchGetItemRequest.class))).thenCallRealMethod();
    service =
        new ScopusMappingService(
            new DynamoDbScopusMappingRepository(client, DEFAULT_TABLE_NAME), Clock.systemUTC());
  }

  @Test
  void shouldAskAgainForUnprocessedKeysUntilEveryKeyIsRead() {
    when(client.batchGetItem(any(BatchGetItemRequest.class)))
        .thenReturn(batchGetResponse(List.of(currentItem(FIRST_AUTHOR)), SECOND_AUTHOR))
        .thenReturn(batchGetResponse(List.of(currentItem(SECOND_AUTHOR))));

    var mappings = service.findCurrentMappings(List.of(FIRST_AUTHOR, SECOND_AUTHOR), emptyList());

    assertThat(mappings.authorMappings()).containsOnlyKeys(FIRST_AUTHOR, SECOND_AUTHOR);
    var requestCaptor = ArgumentCaptor.forClass(BatchGetItemRequest.class);
    verify(client, times(2)).batchGetItem(requestCaptor.capture());
    var keysAskedForAgain =
        requestCaptor.getAllValues().get(1).requestItems().get(DEFAULT_TABLE_NAME);
    assertThat(keysAskedForAgain.keys())
        .containsExactly(currentKey(authorPartitionKey(SECOND_AUTHOR)));
    assertThat(keysAskedForAgain.consistentRead()).isTrue();
  }

  @Test
  void shouldReadWithConsistentReads() {
    when(client.batchGetItem(any(BatchGetItemRequest.class)))
        .thenReturn(batchGetResponse(List.of(currentItem(FIRST_AUTHOR))));

    service.findCurrentMappings(List.of(FIRST_AUTHOR), emptyList());

    var requestCaptor = ArgumentCaptor.forClass(BatchGetItemRequest.class);
    verify(client).batchGetItem(requestCaptor.capture());
    assertThat(requestCaptor.getValue().requestItems().get(DEFAULT_TABLE_NAME).consistentRead())
        .isTrue();
  }

  @Test
  void shouldRejectNullCollectionsBeforeCallingDynamoDb() {
    assertThatNullPointerException()
        .isThrownBy(() -> service.findCurrentMappings(null, emptyList()))
        .withMessage("scopusAuthorIdentifiers");
    assertThatNullPointerException()
        .isThrownBy(() -> service.findCurrentMappings(emptyList(), null))
        .withMessage("scopusAffiliationIdentifiers");
    verifyNoInteractions(client);
  }

  @Test
  void shouldRejectNullIdentifiersBeforeCallingDynamoDb() {
    assertThatNullPointerException()
        .isThrownBy(
            () -> service.findCurrentMappings(Arrays.asList(FIRST_AUTHOR, null), emptyList()))
        .withMessageContaining("scopusAuthorIdentifiers");
    assertThatNullPointerException()
        .isThrownBy(
            () -> service.findCurrentMappings(emptyList(), Arrays.asList(SCOPUS_AFFILIATION, null)))
        .withMessageContaining("scopusAffiliationIdentifiers");
    verifyNoInteractions(client);
  }

  private static Map<String, AttributeValue> currentItem(ScopusAuthorIdentifier scopusAuthor) {
    return ScopusAuthorMappingDao.current(migratedAuthorMapping(scopusAuthor)).toItem();
  }

  private static BatchGetItemResponse batchGetResponse(List<Map<String, AttributeValue>> items) {
    return BatchGetItemResponse.builder().responses(Map.of(DEFAULT_TABLE_NAME, items)).build();
  }

  private static BatchGetItemResponse batchGetResponse(
      List<Map<String, AttributeValue>> items, ScopusAuthorIdentifier unprocessedScopusAuthor) {
    var unprocessedKeys =
        KeysAndAttributes.builder()
            .keys(List.of(currentKey(authorPartitionKey(unprocessedScopusAuthor))))
            .consistentRead(true)
            .build();
    return BatchGetItemResponse.builder()
        .responses(Map.of(DEFAULT_TABLE_NAME, items))
        .unprocessedKeys(Map.of(DEFAULT_TABLE_NAME, unprocessedKeys))
        .build();
  }
}
