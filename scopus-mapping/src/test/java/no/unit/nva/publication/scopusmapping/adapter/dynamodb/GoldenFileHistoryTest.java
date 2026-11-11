package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static java.util.Collections.emptyList;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.AUTHOR_NEEDING_REVIEW;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.CORRECTED_AUTHOR;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.CURATOR_CORRECTED_AUTHOR;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.DELETED_AUTHOR;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.MIGRATED_AUTHOR_NEEDING_REVIEW;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.MIGRATED_VERSION;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.SUB_UNIT_AFFILIATION;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.SUB_UNIT_AFFILIATION_MAPPING;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.TOP_LEVEL_AFFILIATION;
import static no.unit.nva.publication.scopusmapping.adapter.dynamodb.GoldenFileTest.TOP_LEVEL_AFFILIATION_MAPPING;
import static no.unit.nva.publication.scopusmapping.domain.ScopusMappingSource.CURATOR;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.publication.scopusmapping.domain.CristinPersonIdentifier;
import no.unit.nva.publication.scopusmapping.domain.PiaMappingCounts;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingConflictException;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingHistoryEntry;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Reads the history items of the golden file through the use cases. */
class GoldenFileHistoryTest extends ScopusMappingLocalTest {

  private static final String PUBLICATION_COUNT = "publicationCount";
  private static final ScopusAuthorMapping MIGRATED_CORRECTED_AUTHOR =
      new ScopusAuthorMapping(
          CORRECTED_AUTHOR,
          new CristinPersonIdentifier("11111"),
          "Hansen, Kari",
          null,
          false,
          new PiaMappingCounts(3, 3),
          MIGRATED_VERSION);
  private static final ScopusAuthorMapping MIGRATED_DELETED_AUTHOR =
      new ScopusAuthorMapping(
          DELETED_AUTHOR,
          new CristinPersonIdentifier("22222"),
          "Olsen, Per",
          null,
          false,
          new PiaMappingCounts(1, 1),
          MIGRATED_VERSION);
  private static final ScopusAuthorMapping DELETION_OF_AUTHOR =
      new ScopusAuthorMapping(
          DELETED_AUTHOR,
          new CristinPersonIdentifier("22222"),
          "Olsen, Per",
          null,
          false,
          null,
          new ScopusMappingVersion(
              CURATOR, Instant.parse("2026-12-01T09:00:00.000Z"), "7654321@194.0.0.0", 2, true));

  @BeforeEach
  void writeGoldenFileItems() {
    GoldenFile.writeWithBatchWriteItem(client, TABLE_NAME, GoldenFile.readItems());
  }

  @Test
  void shouldReadMigratedAuthorVersionWithEveryCandidate() {
    assertThat(service.findAuthorMappingHistory(AUTHOR_NEEDING_REVIEW))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                MIGRATED_AUTHOR_NEEDING_REVIEW,
                null,
                List.of(
                    authorCandidate("12345", "5", "2025-06-01T10:15:30.000Z"),
                    authorCandidate("67890", "2", "2024-03-15T08:00:00.000Z"))));
  }

  @Test
  void shouldReadCuratorCorrectionWithWhereItWasCorrectedAfterTheMigratedVersion() {
    assertThat(service.findAuthorMappingHistory(CORRECTED_AUTHOR))
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
  void shouldReadDeletionAsTheLastVersion() {
    assertThat(service.findAuthorMappingHistory(DELETED_AUTHOR))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                MIGRATED_DELETED_AUTHOR,
                null,
                List.of(authorCandidate("22222", "1", "2023-09-01T00:00:00.000Z"))),
            new ScopusMappingHistoryEntry<>(DELETION_OF_AUTHOR, null, emptyList()));
  }

  @Test
  void shouldReadMigratedAffiliationVersionsWithEveryCandidate() {
    assertThat(service.findAffiliationMappingHistory(TOP_LEVEL_AFFILIATION))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                TOP_LEVEL_AFFILIATION_MAPPING,
                null,
                List.of(affiliationCandidate("194.0.0.0", "194.0.0.0", "40"))));
    assertThat(service.findAffiliationMappingHistory(SUB_UNIT_AFFILIATION))
        .containsExactly(
            new ScopusMappingHistoryEntry<>(
                SUB_UNIT_AFFILIATION_MAPPING,
                null,
                List.of(
                    affiliationCandidate("185.0.0.0", "185.15.2.10", "3"),
                    affiliationCandidate("194.0.0.0", "194.0.0.0", "2"))));
  }

  @Test
  void shouldReplaceDeletedMappingWhenCuratorCorrectsIt() throws ScopusMappingConflictException {
    var savedMapping =
        service.correctAuthorMapping(
            DELETED_AUTHOR,
            new CristinPersonIdentifier("33333"),
            new ScopusCorrectionContext("2-s2.0-85200000002", 1),
            "1234567@20754.0.0.0",
            Optional.empty());

    assertThat(savedMapping.version().revision()).isEqualTo(3);
    assertThat(savedMapping.version().deleted()).isFalse();
    assertThat(savedMapping.scopusName()).isEqualTo("Olsen, Per");
    assertThat(
            service
                .findCurrentMappings(List.of(DELETED_AUTHOR), emptyList())
                .authorMapping(DELETED_AUTHOR))
        .contains(savedMapping);
  }

  private static Map<String, String> authorCandidate(
      String cristinPersonIdentifier, String publicationCount, String lastChanged) {
    return Map.of(
        "cristinPersonIdentifier",
        cristinPersonIdentifier,
        PUBLICATION_COUNT,
        publicationCount,
        "lastChanged",
        lastChanged);
  }

  private static Map<String, String> affiliationCandidate(
      String cristinInstitutionIdentifier, String cristinUnitIdentifier, String publicationCount) {
    return Map.of(
        "cristinInstitutionIdentifier",
        cristinInstitutionIdentifier,
        "cristinUnitIdentifier",
        cristinUnitIdentifier,
        PUBLICATION_COUNT,
        publicationCount);
  }
}
