package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingLocalTest.TABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.unit.nva.publication.scopusmapping.application.ScopusMappingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

/** Checks how the history is asked for, which the local table can't show. */
class MappingHistoryWithMockedClientTest {

  @Test
  void shouldReadHistoryWithConsistentReads() {
    var client = mock(DynamoDbClient.class);
    when(client.queryPaginator(any(QueryRequest.class))).thenCallRealMethod();
    when(client.query(any(QueryRequest.class)))
        .thenReturn(QueryResponse.builder().items(emptyList()).build());
    var service = new ScopusMappingService(new DynamoDbScopusMappingRepository(client, TABLE_NAME));

    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).isEmpty();

    var requestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(client).query(requestCaptor.capture());
    assertThat(requestCaptor.getValue().consistentRead()).isTrue();
    assertThat(requestCaptor.getValue().tableName()).isEqualTo(TABLE_NAME);
  }
}
