package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR_ID;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.with;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.without;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toHistoryItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.CURATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class ScopusIdentifierMappingRepositoryTest extends ResourcesLocalTest {

  private static final String TABLE_NAME = "import-candidates";
  private static final String OTHER_SCOPUS_AUTHOR_ID = "99999990202";
  private static final String CRISTIN_PERSON_ID = "cristinPersonId";
  private static final String CANDIDATES = "candidates";
  private static final ScopusCorrectionContext FIRST_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000010", 1);

  private ScopusIdentifierMappingRepository repository;

  @BeforeEach
  void setUp() {
    super.init(TABLE_NAME);
    repository = new ScopusIdentifierMappingRepository(client, TABLE_NAME);
  }

  @Test
  void shouldReadMoreIdsThanFitInOneBatchGetItem() {
    var scopusAuthorIds =
        IntStream.range(0, 150).mapToObj(index -> "999999" + (10_000 + index)).toList();
    scopusAuthorIds.forEach(
        scopusAuthorId -> putItem(toCurrentItem(migratedAuthorMapping(scopusAuthorId))));

    var mappings = repository.fetchCurrentMappings(scopusAuthorIds, List.of("99990301"));

    assertThat(mappings.authorMappings()).containsOnlyKeys(scopusAuthorIds);
    assertThat(mappings.affiliationMappings()).isEmpty();
  }

  @Test
  void shouldAcceptDuplicateAndNullIds() {
    var mapping = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    putItem(toCurrentItem(mapping));

    var mappings =
        repository.fetchCurrentMappings(
            Arrays.asList(SCOPUS_AUTHOR_ID, null, SCOPUS_AUTHOR_ID), List.of("1", "1"));

    assertThat(mappings.authorMappings()).isEqualTo(Map.of(SCOPUS_AUTHOR_ID, mapping));
  }

  @Test
  void shouldReturnNoMappingsWhenAskedForNoIds() {
    var mappings = repository.fetchCurrentMappings(emptyList(), emptyList());

    assertThat(mappings.authorMappings()).isEmpty();
    assertThat(mappings.affiliationMappings()).isEmpty();
  }

  @Test
  void shouldLeaveOutMalformedItemButReadTheOtherIds() {
    var otherMapping = migratedAuthorMapping(OTHER_SCOPUS_AUTHOR_ID);
    putItem(
        with(
            toCurrentItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID)),
            "source",
            fromS("SOMETHING_NEW")));
    putItem(toCurrentItem(otherMapping));

    var mappings =
        repository.fetchCurrentMappings(
            List.of(SCOPUS_AUTHOR_ID, OTHER_SCOPUS_AUTHOR_ID), emptyList());

    assertThat(mappings.authorMappings()).isEqualTo(Map.of(OTHER_SCOPUS_AUTHOR_ID, otherMapping));
  }

  @Test
  void shouldReturnHistoryByRevisionEvenWhenTimestampsAreOutOfOrder() {
    var firstVersion = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    var secondVersion =
        new ScopusAuthorMapping(
            SCOPUS_AUTHOR_ID,
            "1002",
            null,
            null,
            null,
            null,
            false,
            CURATOR,
            firstVersion.updatedAt().minusSeconds(60),
            "1234567@20754.0.0.0",
            2,
            false);
    putItem(toHistoryItem(secondVersion, FIRST_CONTEXT));
    putItem(toHistoryItem(firstVersion, FIRST_CONTEXT));

    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(firstVersion, FIRST_CONTEXT, emptyList()),
            new ScopusMappingHistoryEntry<>(secondVersion, FIRST_CONTEXT, emptyList()));
  }

  @Test
  void shouldKeepOnlyScalarValuesOfCandidatesThatAreMaps() {
    var mapping = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    var candidate =
        AttributeValue.fromM(
            Map.of(
                "cristinPersonId", AttributeValue.fromS("1001"),
                "publicationCount", AttributeValue.fromN("4"),
                "stillActive", AttributeValue.fromBool(true),
                "nestedList", AttributeValue.fromL(List.of(AttributeValue.fromS("ignored")))));
    var notACandidate = AttributeValue.fromS("not a map");
    putHistoryItemWithCandidates(mapping, AttributeValue.fromL(List.of(candidate, notACandidate)));

    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .singleElement()
        .extracting(ScopusMappingHistoryEntry::candidates)
        .isEqualTo(
            List.of(
                Map.of("cristinPersonId", "1001", "publicationCount", "4", "stillActive", "true")));
  }

  @Test
  void shouldIgnoreCandidatesThatAreNotAList() {
    putHistoryItemWithCandidates(
        migratedAuthorMapping(SCOPUS_AUTHOR_ID), AttributeValue.fromS("not a list"));

    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .singleElement()
        .extracting(ScopusMappingHistoryEntry::candidates)
        .isEqualTo(emptyList());
  }

  @Test
  void shouldReturnEmptyHistoryForUnknownId() {
    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID)).isEmpty();
    assertThat(repository.fetchAffiliationMappingHistory("99990301")).isEmpty();
  }

  @Test
  void shouldRejectMalformedItemWhenReadingHistory() {
    putItem(
        without(
            toHistoryItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID), FIRST_CONTEXT),
            CRISTIN_PERSON_ID));

    assertThatThrownBy(() -> repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(CRISTIN_PERSON_ID);
  }

  private void putHistoryItemWithCandidates(
      ScopusAuthorMapping mapping, AttributeValue candidates) {
    putItem(with(toHistoryItem(mapping, FIRST_CONTEXT), CANDIDATES, candidates));
  }

  private void putItem(Map<String, AttributeValue> item) {
    client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
  }
}
