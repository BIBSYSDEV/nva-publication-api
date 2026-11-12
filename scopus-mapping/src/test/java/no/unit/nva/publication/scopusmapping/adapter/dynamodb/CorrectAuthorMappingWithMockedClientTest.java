package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.CURATOR_USERNAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingLocalTest.TABLE_NAME;
import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

/** Checks how a correction talks to DynamoDB, which the local table can't show. */
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
}
