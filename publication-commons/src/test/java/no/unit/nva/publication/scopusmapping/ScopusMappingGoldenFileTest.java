package no.unit.nva.publication.scopusmapping;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.CURATOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.PIA_MIGRATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reads the golden file, the contract with the PIA migration script in nva-aws-cli-tools, through
 * the repository. A change to the file is a change to that contract.
 */
class ScopusMappingGoldenFileTest extends ResourcesLocalTest {

  private static final String TABLE_NAME = "import-candidates";
  private static final Instant MIGRATED_AT = Instant.parse("2026-11-02T08:00:00.000Z");
  private static final String MIGRATION_MARKER = "pia-migration";
  private static final String AUTHOR_NEEDING_REVIEW = "99999990101";
  private static final String CORRECTED_AUTHOR = "99999990102";
  private static final String DELETED_AUTHOR = "99999990103";
  private static final String UNKNOWN_AUTHOR = "99999990999";
  private static final String TOP_LEVEL_AFFILIATION = "99990101";
  private static final String SUB_UNIT_AFFILIATION = "99990102";
  private static final String UNKNOWN_AFFILIATION = "99990999";
  private static final String CRISTIN_PERSON_ID = "cristinPersonId";
  private static final String CRISTIN_INSTITUTION_ID = "cristinInstitutionId";
  private static final String CRISTIN_UNIT_ID = "cristinUnitId";
  private static final String PUBLICATION_COUNT = "publicationCount";
  private static final String LAST_CHANGED = "lastChanged";

  private static final ScopusAuthorMapping MIGRATED_AUTHOR_NEEDING_REVIEW =
      new ScopusAuthorMapping(
          AUTHOR_NEEDING_REVIEW,
          "12345",
          "Nordmann, Ola",
          "0000-0002-1825-0097",
          5,
          7,
          true,
          PIA_MIGRATION,
          MIGRATED_AT,
          MIGRATION_MARKER,
          1,
          false);
  private static final ScopusAuthorMapping MIGRATED_CORRECTED_AUTHOR =
      new ScopusAuthorMapping(
          CORRECTED_AUTHOR,
          "11111",
          "Hansen, Kari",
          null,
          3,
          3,
          false,
          PIA_MIGRATION,
          MIGRATED_AT,
          MIGRATION_MARKER,
          1,
          false);
  private static final ScopusAuthorMapping CURATOR_CORRECTED_AUTHOR =
      new ScopusAuthorMapping(
          CORRECTED_AUTHOR,
          "54321",
          "Hansen, Kari",
          null,
          null,
          null,
          false,
          CURATOR,
          Instant.parse("2026-11-20T13:45:00.000Z"),
          "1234567@20754.0.0.0",
          2,
          false);
  private static final ScopusAuthorMapping MIGRATED_DELETED_AUTHOR =
      new ScopusAuthorMapping(
          DELETED_AUTHOR,
          "22222",
          "Olsen, Per",
          null,
          1,
          1,
          false,
          PIA_MIGRATION,
          MIGRATED_AT,
          MIGRATION_MARKER,
          1,
          false);
  private static final ScopusAuthorMapping DELETION_OF_AUTHOR =
      new ScopusAuthorMapping(
          DELETED_AUTHOR,
          "22222",
          "Olsen, Per",
          null,
          null,
          null,
          false,
          CURATOR,
          Instant.parse("2026-12-01T09:00:00.000Z"),
          "7654321@194.0.0.0",
          2,
          true);
  private static final ScopusAffiliationMapping TOP_LEVEL_AFFILIATION_MAPPING =
      new ScopusAffiliationMapping(
          TOP_LEVEL_AFFILIATION,
          "194",
          "194.0.0.0",
          "Norwegian University of Science and Technology",
          40,
          40,
          false,
          PIA_MIGRATION,
          MIGRATED_AT,
          MIGRATION_MARKER,
          1,
          false);
  private static final ScopusAffiliationMapping SUB_UNIT_AFFILIATION_MAPPING =
      new ScopusAffiliationMapping(
          SUB_UNIT_AFFILIATION,
          "185",
          "185.15.2.10",
          "University of Oslo, Department of Informatics",
          3,
          5,
          true,
          PIA_MIGRATION,
          MIGRATED_AT,
          MIGRATION_MARKER,
          1,
          false);

  private ScopusIdentifierMappingRepository repository;

  @BeforeEach
  void setUp() {
    super.init(TABLE_NAME);
    ScopusMappingTestItems.writeWithBatchWriteItem(
        client, TABLE_NAME, ScopusMappingTestItems.readGoldenFile());
    repository = new ScopusIdentifierMappingRepository(client, TABLE_NAME);
  }

  @Test
  void shouldReadCurrentAuthorMappingsThatAreNotDeleted() {
    var mappings = fetchEveryIdInTheFileAndUnknownIds();

    assertThat(mappings.authorMappings())
        .isEqualTo(
            Map.of(
                AUTHOR_NEEDING_REVIEW, MIGRATED_AUTHOR_NEEDING_REVIEW,
                CORRECTED_AUTHOR, CURATOR_CORRECTED_AUTHOR));
  }

  @Test
  void shouldReadCurrentAffiliationMappingsAtTopLevelAndSubUnit() {
    var mappings = fetchEveryIdInTheFileAndUnknownIds();

    assertThat(mappings.affiliationMappings())
        .isEqualTo(
            Map.of(
                TOP_LEVEL_AFFILIATION, TOP_LEVEL_AFFILIATION_MAPPING,
                SUB_UNIT_AFFILIATION, SUB_UNIT_AFFILIATION_MAPPING));
  }

  @Test
  void shouldLeaveOutDeletedAndUnknownIds() {
    var mappings = fetchEveryIdInTheFileAndUnknownIds();

    assertThat(mappings.authorMapping(DELETED_AUTHOR)).isEmpty();
    assertThat(mappings.authorMapping(UNKNOWN_AUTHOR)).isEmpty();
    assertThat(mappings.affiliationMapping(UNKNOWN_AFFILIATION)).isEmpty();
  }

  @Test
  void shouldReadMigratedAuthorHistoryWithEveryCandidate() {
    var history = repository.fetchAuthorMappingHistory(AUTHOR_NEEDING_REVIEW);

    assertThat(history)
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                MIGRATED_AUTHOR_NEEDING_REVIEW,
                null,
                List.of(
                    authorCandidate("12345", "5", "2025-06-01T10:15:30.000Z"),
                    authorCandidate("67890", "2", "2024-03-15T08:00:00.000Z"))));
  }

  @Test
  void shouldReadCuratorCorrectionWithItsContextAfterTheMigratedVersion() {
    var history = repository.fetchAuthorMappingHistory(CORRECTED_AUTHOR);

    assertThat(history)
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                MIGRATED_CORRECTED_AUTHOR,
                null,
                List.of(authorCandidate("11111", "3", "2025-01-10T12:00:00.000Z"))),
            new ScopusMappingHistoryEntry<>(
                CURATOR_CORRECTED_AUTHOR,
                new ScopusCorrectionContext("2-s2.0-85200000001", 3),
                emptyList()));
  }

  @Test
  void shouldReadDeletedFlagOfDeletedMapping() {
    var history = repository.fetchAuthorMappingHistory(DELETED_AUTHOR);

    assertThat(history)
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                MIGRATED_DELETED_AUTHOR,
                null,
                List.of(authorCandidate("22222", "1", "2023-09-01T00:00:00.000Z"))),
            new ScopusMappingHistoryEntry<>(DELETION_OF_AUTHOR, null, emptyList()));
  }

  @Test
  void shouldReadMigratedAffiliationHistoryWithEveryCandidate() {
    assertThat(repository.fetchAffiliationMappingHistory(TOP_LEVEL_AFFILIATION))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                TOP_LEVEL_AFFILIATION_MAPPING,
                null,
                List.of(affiliationCandidate("194", "194.0.0.0", "40"))));
    assertThat(repository.fetchAffiliationMappingHistory(SUB_UNIT_AFFILIATION))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                SUB_UNIT_AFFILIATION_MAPPING,
                null,
                List.of(
                    affiliationCandidate("185", "185.15.2.10", "3"),
                    affiliationCandidate("194", "194.0.0.0", "2"))));
  }

  @Test
  void shouldReplaceDeletedMappingWhenCallerExpectsNoMapping()
      throws ScopusMappingConflictException {
    var savedMapping =
        repository.saveCristinPersonId(
            DELETED_AUTHOR,
            "33333",
            new ScopusCorrectionContext("2-s2.0-85200000002", 1),
            "1234567@20754.0.0.0",
            Optional.empty());

    assertThat(savedMapping.revision()).isEqualTo(DELETION_OF_AUTHOR.revision() + 1);
    assertThat(savedMapping.deleted()).isFalse();
    assertThat(savedMapping.scopusName()).isEqualTo(DELETION_OF_AUTHOR.scopusName());
    assertThat(
            repository
                .fetchCurrentMappings(List.of(DELETED_AUTHOR), emptyList())
                .authorMapping(DELETED_AUTHOR))
        .contains(savedMapping);
  }

  private static Map<String, String> authorCandidate(
      String cristinPersonId, String publicationCount, String lastChanged) {
    return Map.of(
        CRISTIN_PERSON_ID,
        cristinPersonId,
        PUBLICATION_COUNT,
        publicationCount,
        LAST_CHANGED,
        lastChanged);
  }

  private static Map<String, String> affiliationCandidate(
      String cristinInstitutionId, String cristinUnitId, String publicationCount) {
    return Map.of(
        CRISTIN_INSTITUTION_ID,
        cristinInstitutionId,
        CRISTIN_UNIT_ID,
        cristinUnitId,
        PUBLICATION_COUNT,
        publicationCount);
  }

  private ScopusMappings fetchEveryIdInTheFileAndUnknownIds() {
    return repository.fetchCurrentMappings(
        List.of(AUTHOR_NEEDING_REVIEW, CORRECTED_AUTHOR, DELETED_AUTHOR, UNKNOWN_AUTHOR),
        List.of(TOP_LEVEL_AFFILIATION, SUB_UNIT_AFFILIATION, UNKNOWN_AFFILIATION));
  }
}
