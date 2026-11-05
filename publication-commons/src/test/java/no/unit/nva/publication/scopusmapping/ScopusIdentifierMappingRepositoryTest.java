package no.unit.nva.publication.scopusmapping;

import static java.net.HttpURLConnection.HTTP_CONFLICT;
import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.CURATOR_USERNAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.FIXTURES;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_AT;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR_ID;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.TABLE_NAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.curatorAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.with;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.without;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.authorPartitionKey;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toCurrentItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toHistoryItem;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.CURATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.junit.jupiter.api.Named.named;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.unit.nva.publication.service.ResourcesLocalTest;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.logutils.LogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

class ScopusIdentifierMappingRepositoryTest extends ResourcesLocalTest {

  private static final Instant NOW = Instant.parse("2026-11-04T12:00:00.123Z");
  private static final String FIRST_PERSON = "1001";
  private static final String SECOND_PERSON = "1002";
  private static final String THIRD_PERSON = "1003";
  private static final String OTHER_SCOPUS_AUTHOR_ID = "99999990202";
  private static final String CRISTIN_PERSON_ID = "cristinPersonId";
  private static final String CANDIDATES = "candidates";
  private static final String REVISION = "revision";
  private static final ScopusCorrectionContext FIRST_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000010", 1);
  private static final ScopusCorrectionContext SECOND_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000011", 2);
  private static final ScopusCorrectionContext THIRD_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000012", 3);

  private ScopusIdentifierMappingRepository repository;

  @BeforeEach
  void setUp() {
    super.init(TABLE_NAME);
    repository =
        new ScopusIdentifierMappingRepository(
            client, TABLE_NAME, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ZERO);
  }

  @Test
  void shouldCreateMappingWhenNoneExists() throws ScopusMappingConflictException {
    var savedMapping = saveFirstPersonAsNewMapping();

    var expectedMapping = curatorMapping(FIRST_PERSON, 1);
    assertThat(savedMapping).isEqualTo(expectedMapping);
    assertThat(fetchCurrentAuthorMapping()).contains(expectedMapping);
    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(expectedMapping, FIRST_CONTEXT, emptyList()));
  }

  @Test
  void shouldReplaceMappingWhenExpectedRevisionMatches() throws ScopusMappingConflictException {
    saveFirstPersonAsNewMapping();

    var savedMapping =
        repository.saveCristinPersonId(
            SCOPUS_AUTHOR_ID, SECOND_PERSON, SECOND_CONTEXT, CURATOR_USERNAME, Optional.of(1));

    assertThat(savedMapping).isEqualTo(curatorMapping(SECOND_PERSON, 2));
    assertThat(fetchCurrentAuthorMapping()).contains(savedMapping);
  }

  @Test
  void shouldThrowConflictAndKeepMappingWhenExpectedRevisionIsStale()
      throws ScopusMappingConflictException {
    saveFirstPersonAsNewMapping();
    var currentMapping =
        repository.saveCristinPersonId(
            SCOPUS_AUTHOR_ID, SECOND_PERSON, SECOND_CONTEXT, CURATOR_USERNAME, Optional.of(1));

    assertThatThrownBy(
            () ->
                repository.saveCristinPersonId(
                    SCOPUS_AUTHOR_ID,
                    THIRD_PERSON,
                    THIRD_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.of(1)))
        .isInstanceOf(ScopusMappingConflictException.class)
        .hasMessageContaining(SCOPUS_AUTHOR_ID)
        .asInstanceOf(type(ConflictException.class))
        .extracting(ConflictException::getStatusCode)
        .isEqualTo(HTTP_CONFLICT);
    assertThat(fetchCurrentAuthorMapping()).contains(currentMapping);
    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID)).hasSize(2);
  }

  @Test
  void shouldWriteNewVersionThatClearsNeedsReviewWhenCuratorConfirmsTheMappedPerson()
      throws ScopusMappingConflictException {
    var migratedMapping = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    putItem(toCurrentItem(migratedMapping));

    var confirmedMapping =
        repository.saveCristinPersonId(
            SCOPUS_AUTHOR_ID,
            migratedMapping.cristinPersonId(),
            SECOND_CONTEXT,
            CURATOR_USERNAME,
            Optional.of(migratedMapping.revision()));

    assertThat(migratedMapping.needsReview()).isTrue();
    assertThat(confirmedMapping.needsReview()).isFalse();
    assertThat(confirmedMapping.source()).isEqualTo(CURATOR);
    assertThat(confirmedMapping.revision()).isEqualTo(migratedMapping.revision() + 1);
    assertThat(fetchCurrentAuthorMapping()).contains(confirmedMapping);
    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(confirmedMapping, SECOND_CONTEXT, emptyList()));
  }

  @Test
  void shouldAddOneHistoryItemPerWrite() throws ScopusMappingConflictException {
    saveFirstPersonAsNewMapping();
    repository.saveCristinPersonId(
        SCOPUS_AUTHOR_ID, SECOND_PERSON, SECOND_CONTEXT, CURATOR_USERNAME, Optional.of(1));
    repository.saveCristinPersonId(
        SCOPUS_AUTHOR_ID, THIRD_PERSON, THIRD_CONTEXT, CURATOR_USERNAME, Optional.of(2));

    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                curatorMapping(FIRST_PERSON, 1), FIRST_CONTEXT, emptyList()),
            new ScopusMappingHistoryEntry<>(
                curatorMapping(SECOND_PERSON, 2), SECOND_CONTEXT, emptyList()),
            new ScopusMappingHistoryEntry<>(
                curatorMapping(THIRD_PERSON, 3), THIRD_CONTEXT, emptyList()));
  }

  @Test
  void shouldThrowConflictWhenMappingExistsButCallerExpectsNone()
      throws ScopusMappingConflictException {
    saveFirstPersonAsNewMapping();

    assertThatThrownBy(
            () ->
                repository.saveCristinPersonId(
                    SCOPUS_AUTHOR_ID,
                    SECOND_PERSON,
                    SECOND_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.empty()))
        .isInstanceOf(ScopusMappingConflictException.class);
  }

  @Test
  void shouldThrowConflictWhenCallerExpectsRevisionButNoMappingExists() {
    assertThatThrownBy(
            () ->
                repository.saveCristinPersonId(
                    SCOPUS_AUTHOR_ID,
                    FIRST_PERSON,
                    FIRST_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.of(1)))
        .isInstanceOf(ScopusMappingConflictException.class);
    assertThat(fetchCurrentAuthorMapping()).isEmpty();
    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID)).isEmpty();
  }

  @Test
  void shouldKeepScopusNameAndOrcidButNotMigrationFieldsWhenCuratorReplacesMigratedMapping()
      throws ScopusMappingConflictException {
    putItem(toCurrentItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID)));

    var savedMapping =
        repository.saveCristinPersonId(
            SCOPUS_AUTHOR_ID, SECOND_PERSON, SECOND_CONTEXT, CURATOR_USERNAME, Optional.of(1));

    assertThat(savedMapping)
        .isEqualTo(
            new ScopusAuthorMapping(
                SCOPUS_AUTHOR_ID,
                SECOND_PERSON,
                "Nordmann, Ola",
                "0000-0002-1825-0097",
                null,
                null,
                false,
                CURATOR,
                NOW,
                CURATOR_USERNAME,
                2,
                false));
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

  @ParameterizedTest
  @MethodSource(FIXTURES + "#malformedAuthorItemsWithReadableRevision")
  void shouldReplaceMalformedMappingKeepingItsScopusNameAndOrcidWhenCallerExpectsNoMapping(
      Map<String, AttributeValue> malformedItem, String expectedReason)
      throws ScopusMappingConflictException {
    var logRecorder = LogRecorder.forClass(ScopusMappingItems.class);
    var mappingBeforeItWasMalformed = migratedAuthorMapping(SCOPUS_AUTHOR_ID);
    putItem(malformedItem);

    var savedMapping =
        repository.saveCristinPersonId(
            SCOPUS_AUTHOR_ID, SECOND_PERSON, SECOND_CONTEXT, CURATOR_USERNAME, Optional.empty());

    assertThat(savedMapping.cristinPersonId()).isEqualTo(SECOND_PERSON);
    assertThat(savedMapping.revision()).isEqualTo(mappingBeforeItWasMalformed.revision() + 1);
    assertThat(savedMapping.scopusName()).isEqualTo(mappingBeforeItWasMalformed.scopusName());
    assertThat(savedMapping.orcid()).isEqualTo(mappingBeforeItWasMalformed.orcid());
    assertThat(fetchCurrentAuthorMapping()).contains(savedMapping);
    assertThat(logRecorder.messages()).singleElement().asString().contains(expectedReason);
  }

  @Test
  void shouldFailNamingTheItemWithoutWritingWhenMalformedMappingHasNoReadableRevision() {
    putItem(with(toCurrentItem(migratedAuthorMapping(SCOPUS_AUTHOR_ID)), REVISION, fromS("1")));

    assertThatThrownBy(
            () ->
                repository.saveCristinPersonId(
                    SCOPUS_AUTHOR_ID,
                    SECOND_PERSON,
                    SECOND_CONTEXT,
                    CURATOR_USERNAME,
                    Optional.empty()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(authorPartitionKey(SCOPUS_AUTHOR_ID))
        .hasMessageContaining(REVISION);
    assertThat(repository.fetchAuthorMappingHistory(SCOPUS_AUTHOR_ID)).isEmpty();
  }

  private static ScopusAuthorMapping curatorMapping(String cristinPersonId, int revision) {
    return curatorAuthorMapping(SCOPUS_AUTHOR_ID, cristinPersonId, NOW, revision, false);
  }

  private ScopusAuthorMapping saveFirstPersonAsNewMapping() throws ScopusMappingConflictException {
    return repository.saveCristinPersonId(
        SCOPUS_AUTHOR_ID, FIRST_PERSON, FIRST_CONTEXT, CURATOR_USERNAME, Optional.empty());
  }

  private Optional<ScopusAuthorMapping> fetchCurrentAuthorMapping() {
    return repository
        .fetchCurrentMappings(List.of(SCOPUS_AUTHOR_ID), emptyList())
        .authorMapping(SCOPUS_AUTHOR_ID);
  }

  private void putItem(Map<String, AttributeValue> item) {
    client.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
  }
}
