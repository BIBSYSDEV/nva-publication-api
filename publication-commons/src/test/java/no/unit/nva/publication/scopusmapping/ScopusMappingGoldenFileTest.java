package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.ScopusMappingFixtures.TABLE_NAME;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.CURATOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.PIA_MIGRATION;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import no.unit.nva.publication.service.ResourcesLocalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Reads the golden file, the contract with the PIA migration script in nva-aws-cli-tools, through
 * the repository. A change to the file is a change to that contract.
 */
class ScopusMappingGoldenFileTest extends ResourcesLocalTest {

  private static final Instant MIGRATED_AT = Instant.parse("2026-11-02T08:00:00.000Z");
  private static final String MIGRATION_MARKER = "pia-migration";
  private static final String AUTHOR_NEEDING_REVIEW = "99999990101";
  private static final String CORRECTED_AUTHOR = "99999990102";
  private static final String DELETED_AUTHOR = "99999990103";
  private static final String UNKNOWN_AUTHOR = "99999990999";
  private static final String TOP_LEVEL_AFFILIATION = "99990101";
  private static final String SUB_UNIT_AFFILIATION = "99990102";
  private static final String UNKNOWN_AFFILIATION = "99990999";

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

  private ScopusMappings fetchEveryIdInTheFileAndUnknownIds() {
    return repository.fetchCurrentMappings(
        List.of(AUTHOR_NEEDING_REVIEW, CORRECTED_AUTHOR, DELETED_AUTHOR, UNKNOWN_AUTHOR),
        List.of(TOP_LEVEL_AFFILIATION, SUB_UNIT_AFFILIATION, UNKNOWN_AFFILIATION));
  }
}
