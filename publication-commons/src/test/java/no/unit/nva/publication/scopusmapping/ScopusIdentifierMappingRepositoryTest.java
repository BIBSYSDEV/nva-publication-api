package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_AT;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR_ID;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.TABLE_NAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.curatorAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.with;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.without;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toHistoryItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class ScopusIdentifierMappingRepositoryTest extends ResourcesLocalTest {

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
  void shouldAcceptDuplicateIds() {
    var mapping = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    putItem(toCurrentItem(mapping));

    var mappings =
        repository.fetchCurrentMappings(
            List.of(SCOPUS_AUTHOR_ID, SCOPUS_AUTHOR_ID), List.of("1", "1"));

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
        curatorAuthorMapping(
            SCOPUS_AUTHOR_ID, "1002", firstVersion.updatedAt().minusSeconds(60), 2, false);
    putItem(toHistoryItem(secondVersion, FIRST_CONTEXT));
    putItem(toHistoryItem(firstVersion, FIRST_CONTEXT));

    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(firstVersion, FIRST_CONTEXT, emptyList()),
            new ScopusMappingHistoryEntry<>(secondVersion, FIRST_CONTEXT, emptyList()));
  }

  @Test
  void shouldReadCandidateValuesAsText() {
    var candidate =
        AttributeValue.fromM(
            Map.of(
                "cristinPersonId", AttributeValue.fromS("1001"),
                "publicationCount", AttributeValue.fromN("4"),
                "stillActive", AttributeValue.fromBool(true)));
    putItem(
        with(
            toHistoryItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID), FIRST_CONTEXT),
            CANDIDATES,
            AttributeValue.fromL(List.of(candidate))));

    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .singleElement()
        .extracting(ScopusMappingHistoryEntry::candidates)
        .isEqualTo(
            List.of(
                Map.of("cristinPersonId", "1001", "publicationCount", "4", "stillActive", "true")));
  }

  @Test
  void shouldReturnEmptyHistoryForUnknownId() {
    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID)).isEmpty();
    assertThat(repository.fetchAffiliationMappingHistory("99990301")).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("malformedHistoryItems")
  void shouldLeaveOutAndLogMalformedHistoryItemButReadTheOtherVersions(
      Map<String, AttributeValue> malformedSecondVersion, String expectedReason) {
    var logRecorder = LogRecorder.forClass(ScopusMappingItems.class);
    var firstVersion = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    putItem(toHistoryItem(firstVersion, FIRST_CONTEXT));
    putItem(malformedSecondVersion);

    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .containsExactly(new ScopusMappingHistoryEntry<>(firstVersion, FIRST_CONTEXT, emptyList()));
    assertThat(logRecorder.messages()).singleElement().asString().contains(expectedReason);
  }

  private static Stream<Arguments> malformedHistoryItems() {
    var secondVersion =
        toHistoryItem(
            curatorAuthorMapping(SCOPUS_AUTHOR_ID, "1002", MIGRATED_AT.plusSeconds(60), 2, false),
            FIRST_CONTEXT);
    return Stream.of(
        Arguments.of(
            named("missing cristinPersonId", without(secondVersion, CRISTIN_PERSON_ID)),
            CRISTIN_PERSON_ID),
        Arguments.of(
            named("revision stored as a string", with(secondVersion, "revision", fromS("2"))),
            "revision"),
        Arguments.of(
            named(
                "candidates that are not a list",
                with(secondVersion, CANDIDATES, fromS("not a list"))),
            "Cannot deserialize"));
  }

  private void putItem(Map<String, AttributeValue> item) {
    client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
  }
}
