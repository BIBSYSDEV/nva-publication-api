package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.CURATOR_USERNAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_AT;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.MIGRATED_PERSON;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.SCOPUS_AUTHOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.curatorAuthorMapping;
import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.migratedAuthorMapping;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.FIXTURES;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.REVISION;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.migratedAuthorItem;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.ScopusMappingItemFixtures.with;
import static no.unit.nva.publication.scopusmapping.domain.ScopusMappingSource.CURATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.services.dynamodb.model.AttributeValue.fromN;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.publication.scopusmapping.domain.CristinPersonIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingConflictException;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingHistoryEntry;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;

class CorrectAuthorMappingTest extends ScopusMappingLocalTest {

  private static final String AUTHOR_PARTITION_KEY = "AUTHOR#SCOPUS#" + SCOPUS_AUTHOR.value();
  private static final CristinPersonIdentifier FIRST_PERSON = new CristinPersonIdentifier("1001");
  private static final CristinPersonIdentifier SECOND_PERSON = new CristinPersonIdentifier("1002");
  private static final CristinPersonIdentifier THIRD_PERSON = new CristinPersonIdentifier("1003");
  private static final ScopusCorrectionContext FIRST_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000010", 1);
  private static final ScopusCorrectionContext SECOND_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000011", 2);
  private static final ScopusCorrectionContext THIRD_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000012", 3);

  @Test
  void shouldCreateMappingWhenNoneExists() throws ScopusMappingConflictException {
    var savedMapping = correct(FIRST_PERSON, FIRST_CONTEXT, Optional.empty());

    var expectedMapping = curatorMapping(FIRST_PERSON, 1);
    assertThat(savedMapping).isEqualTo(expectedMapping);
    assertThat(findCurrentMapping()).contains(expectedMapping);
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(expectedMapping, FIRST_CONTEXT, emptyList()));
  }

  @Test
  void shouldReplaceMappingWhenExpectedRevisionMatches() throws ScopusMappingConflictException {
    correct(FIRST_PERSON, FIRST_CONTEXT, Optional.empty());

    var savedMapping = correct(SECOND_PERSON, SECOND_CONTEXT, Optional.of(1));

    assertThat(savedMapping).isEqualTo(curatorMapping(SECOND_PERSON, 2));
    assertThat(findCurrentMapping()).contains(savedMapping);
  }

  @Test
  void shouldKeepEveryVersionInTheHistory() throws ScopusMappingConflictException {
    correct(FIRST_PERSON, FIRST_CONTEXT, Optional.empty());
    correct(SECOND_PERSON, SECOND_CONTEXT, Optional.of(1));
    correct(THIRD_PERSON, THIRD_CONTEXT, Optional.of(2));

    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                curatorMapping(FIRST_PERSON, 1), FIRST_CONTEXT, emptyList()),
            new ScopusMappingHistoryEntry<>(
                curatorMapping(SECOND_PERSON, 2), SECOND_CONTEXT, emptyList()),
            new ScopusMappingHistoryEntry<>(
                curatorMapping(THIRD_PERSON, 3), THIRD_CONTEXT, emptyList()));
  }

  @Test
  void shouldThrowConflictAndKeepMappingWhenExpectedRevisionIsStale()
      throws ScopusMappingConflictException {
    correct(FIRST_PERSON, FIRST_CONTEXT, Optional.empty());
    var currentMapping = correct(SECOND_PERSON, SECOND_CONTEXT, Optional.of(1));

    assertThatThrownBy(() -> correct(THIRD_PERSON, THIRD_CONTEXT, Optional.of(1)))
        .isInstanceOf(ScopusMappingConflictException.class)
        .hasMessageContaining(SCOPUS_AUTHOR.value());
    assertThat(findCurrentMapping()).contains(currentMapping);
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).hasSize(2);
  }

  @Test
  void shouldThrowConflictWhenMappingExistsButCallerExpectsNone()
      throws ScopusMappingConflictException {
    var currentMapping = correct(FIRST_PERSON, FIRST_CONTEXT, Optional.empty());

    assertThatThrownBy(() -> correct(SECOND_PERSON, SECOND_CONTEXT, Optional.empty()))
        .isInstanceOf(ScopusMappingConflictException.class);
    assertThat(findCurrentMapping()).contains(currentMapping);
  }

  @Test
  void shouldThrowConflictWhenCallerExpectsRevisionButNoMappingExists() {
    assertThatThrownBy(() -> correct(FIRST_PERSON, FIRST_CONTEXT, Optional.of(1)))
        .isInstanceOf(ScopusMappingConflictException.class);
    assertThat(findCurrentMapping()).isEmpty();
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).isEmpty();
  }

  @Test
  void shouldReplaceDeletedMappingWhenCallerExpectsNone() throws ScopusMappingConflictException {
    putCurrent(curatorAuthorMapping(SCOPUS_AUTHOR, "1001", MIGRATED_AT, 2, true));

    var savedMapping = correct(SECOND_PERSON, SECOND_CONTEXT, Optional.empty());

    assertThat(savedMapping).isEqualTo(curatorMapping(SECOND_PERSON, 3));
    assertThat(findCurrentMapping()).contains(savedMapping);
  }

  @Test
  void shouldKeepWhatDescribesTheScopusAuthorButNotWhatTheMigrationSet()
      throws ScopusMappingConflictException {
    var migratedMapping = migratedAuthorMapping(SCOPUS_AUTHOR);
    putCurrent(migratedMapping);

    var savedMapping = correct(SECOND_PERSON, SECOND_CONTEXT, Optional.of(1));

    assertThat(savedMapping)
        .isEqualTo(
            new ScopusAuthorMapping(
                SCOPUS_AUTHOR,
                SECOND_PERSON,
                migratedMapping.scopusName(),
                migratedMapping.orcid(),
                false,
                null,
                new ScopusMappingVersion(CURATOR, NOW, CURATOR_USERNAME, 2, false)));
  }

  @Test
  void shouldSaveNewVersionThatClearsTheReviewFlagWhenCuratorConfirmsTheMappedPerson()
      throws ScopusMappingConflictException {
    var migratedMapping = migratedAuthorMapping(SCOPUS_AUTHOR);
    putCurrent(migratedMapping);

    var confirmedMapping = correct(MIGRATED_PERSON, SECOND_CONTEXT, Optional.of(1));

    assertThat(migratedMapping.needsReview()).isTrue();
    assertThat(confirmedMapping.needsReview()).isFalse();
    assertThat(confirmedMapping.version().source()).isEqualTo(CURATOR);
    assertThat(confirmedMapping.version().revision()).isEqualTo(2);
    assertThat(findCurrentMapping()).contains(confirmedMapping);
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(confirmedMapping, SECOND_CONTEXT, emptyList()));
  }

  @ParameterizedTest
  @MethodSource(FIXTURES + "#malformedAuthorItems")
  void shouldFailNamingTheItemWithoutSavingWhenStoredMappingCanNotBeRead(
      Map<String, AttributeValue> malformedItem, String expectedReason) {
    putItem(malformedItem);

    assertThatThrownBy(() -> correct(SECOND_PERSON, SECOND_CONTEXT, Optional.empty()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(AUTHOR_PARTITION_KEY)
        .hasMessageContaining(expectedReason);
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).isEmpty();
  }

  @Test
  void shouldFailNamingTheItemWhenStoredRevisionIsBelowTheFirst() {
    putItem(with(migratedAuthorItem(), REVISION, fromN("0")));

    assertThatThrownBy(() -> correct(SECOND_PERSON, SECOND_CONTEXT, Optional.empty()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(AUTHOR_PARTITION_KEY)
        .hasMessageContaining("revision must be at least 1");
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).isEmpty();
  }

  @Test
  void shouldRefuseToSaveVersionWhenTheStoredRevisionIsNotTheOneBeforeIt() {
    var storedMapping = curatorMapping(FIRST_PERSON, 2);
    putCurrent(storedMapping);
    var versionBuiltOnAnOlderRevision = curatorMapping(SECOND_PERSON, 2);

    assertThatThrownBy(
            () -> repository.saveCorrection(versionBuiltOnAnOlderRevision, SECOND_CONTEXT))
        .isInstanceOf(TransactionCanceledException.class);
    assertThat(findCurrentMapping()).contains(storedMapping);
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).isEmpty();
  }

  @Test
  void shouldRefuseToSaveFirstVersionWhenAMappingIsAlreadyStored() {
    var storedMapping = curatorMapping(FIRST_PERSON, 1);
    putCurrent(storedMapping);
    var firstVersionOfAnotherCurator = curatorMapping(SECOND_PERSON, 1);

    assertThatThrownBy(
            () -> repository.saveCorrection(firstVersionOfAnotherCurator, SECOND_CONTEXT))
        .isInstanceOf(TransactionCanceledException.class);
    assertThat(findCurrentMapping()).contains(storedMapping);
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).isEmpty();
  }

  @Test
  void shouldRejectNullArguments() {
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                service.correctAuthorMapping(
                    null, FIRST_PERSON, FIRST_CONTEXT, CURATOR_USERNAME, Optional.empty()))
        .withMessage("scopusAuthorIdentifier");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                service.correctAuthorMapping(
                    SCOPUS_AUTHOR, null, FIRST_CONTEXT, CURATOR_USERNAME, Optional.empty()))
        .withMessage("cristinPersonIdentifier");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                service.correctAuthorMapping(
                    SCOPUS_AUTHOR, FIRST_PERSON, null, CURATOR_USERNAME, Optional.empty()))
        .withMessage("correctionContext");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                service.correctAuthorMapping(
                    SCOPUS_AUTHOR, FIRST_PERSON, FIRST_CONTEXT, null, Optional.empty()))
        .withMessage("username");
    assertThatNullPointerException()
        .isThrownBy(
            () ->
                service.correctAuthorMapping(
                    SCOPUS_AUTHOR, FIRST_PERSON, FIRST_CONTEXT, CURATOR_USERNAME, null))
        .withMessage("expectedRevision");
    assertThat(service.findAuthorMappingHistory(SCOPUS_AUTHOR)).isEmpty();
  }

  private static ScopusAuthorMapping curatorMapping(
      CristinPersonIdentifier cristinPerson, int revision) {
    return curatorAuthorMapping(SCOPUS_AUTHOR, cristinPerson.value(), NOW, revision, false);
  }

  private ScopusAuthorMapping correct(
      CristinPersonIdentifier cristinPerson,
      ScopusCorrectionContext correctionContext,
      Optional<Integer> expectedRevision)
      throws ScopusMappingConflictException {
    return service.correctAuthorMapping(
        SCOPUS_AUTHOR, cristinPerson, correctionContext, CURATOR_USERNAME, expectedRevision);
  }

  private Optional<ScopusAuthorMapping> findCurrentMapping() {
    return service
        .findCurrentMappings(List.of(SCOPUS_AUTHOR), emptyList())
        .authorMapping(SCOPUS_AUTHOR);
  }

  private void putCurrent(ScopusAuthorMapping mapping) {
    putItem(ScopusAuthorMappingDao.current(mapping).toItem());
  }
}
