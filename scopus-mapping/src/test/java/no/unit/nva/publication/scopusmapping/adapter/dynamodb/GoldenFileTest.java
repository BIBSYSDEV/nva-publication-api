package no.unit.nva.publication.scopusmapping.adapter.dynamodb;

import static no.unit.nva.publication.scopusmapping.domain.ScopusMappingSource.CURATOR;
import static no.unit.nva.publication.scopusmapping.domain.ScopusMappingSource.PIA_MIGRATION;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_PARTITION_KEY_NAME;
import static no.unit.nva.publication.storage.model.DatabaseConstants.PRIMARY_KEY_SORT_KEY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import no.unit.nva.publication.scopusmapping.domain.CristinOrganizationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.CristinPersonIdentifier;
import no.unit.nva.publication.scopusmapping.domain.PiaMappingCounts;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingVersion;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/**
 * Reads the golden file, the contract with the PIA migration script in nva-aws-cli-tools, through
 * the use cases. A change to the file is a change to that contract. The expected values are spelled
 * out here, not taken from shared fixtures, so the test shows what the file means. The items a
 * curator's correction writes are in the file too, so what Java writes is held to the same format.
 */
class GoldenFileTest extends ScopusMappingLocalTest {

  static final ScopusAuthorIdentifier AUTHOR_NEEDING_REVIEW =
      new ScopusAuthorIdentifier("99999990101");
  static final ScopusAuthorIdentifier CORRECTED_AUTHOR = new ScopusAuthorIdentifier("99999990102");
  static final ScopusAuthorIdentifier DELETED_AUTHOR = new ScopusAuthorIdentifier("99999990103");
  static final ScopusAffiliationIdentifier TOP_LEVEL_AFFILIATION =
      new ScopusAffiliationIdentifier("99990101");
  static final ScopusAffiliationIdentifier SUB_UNIT_AFFILIATION =
      new ScopusAffiliationIdentifier("99990102");
  static final ScopusMappingVersion MIGRATED_VERSION =
      new ScopusMappingVersion(
          PIA_MIGRATION, Instant.parse("2026-11-02T08:00:00.000Z"), "pia-migration", 1, false);
  static final ScopusAuthorMapping MIGRATED_AUTHOR_NEEDING_REVIEW =
      new ScopusAuthorMapping(
          AUTHOR_NEEDING_REVIEW,
          new CristinPersonIdentifier("12345"),
          "Nordmann, Ola",
          "0000-0002-1825-0097",
          true,
          new PiaMappingCounts(5, 7),
          MIGRATED_VERSION);
  static final ScopusAuthorMapping CURATOR_CORRECTED_AUTHOR =
      new ScopusAuthorMapping(
          CORRECTED_AUTHOR,
          new CristinPersonIdentifier("54321"),
          "Hansen, Kari",
          null,
          false,
          null,
          new ScopusMappingVersion(
              CURATOR, Instant.parse("2026-11-20T13:45:00.000Z"), "1234567@20754.0.0.0", 2, false));
  static final ScopusAffiliationMapping TOP_LEVEL_AFFILIATION_MAPPING =
      new ScopusAffiliationMapping(
          TOP_LEVEL_AFFILIATION,
          new CristinOrganizationIdentifier("194.0.0.0"),
          new CristinOrganizationIdentifier("194.0.0.0"),
          "Norwegian University of Science and Technology",
          false,
          new PiaMappingCounts(40, 40),
          MIGRATED_VERSION);
  static final ScopusAffiliationMapping SUB_UNIT_AFFILIATION_MAPPING =
      new ScopusAffiliationMapping(
          SUB_UNIT_AFFILIATION,
          new CristinOrganizationIdentifier("185.0.0.0"),
          new CristinOrganizationIdentifier("185.15.2.10"),
          "University of Oslo, Department of Informatics",
          true,
          new PiaMappingCounts(3, 5),
          MIGRATED_VERSION);
  private static final String CORRECTED_AUTHOR_PARTITION_KEY = "AUTHOR#SCOPUS#99999990102";
  private static final ScopusCorrectionContext CORRECTION_CONTEXT =
      new ScopusCorrectionContext("2-s2.0-85200000001", 3);
  private static final ScopusAuthorIdentifier UNKNOWN_AUTHOR =
      new ScopusAuthorIdentifier("99999990999");
  private static final ScopusAffiliationIdentifier UNKNOWN_AFFILIATION =
      new ScopusAffiliationIdentifier("99990999");

  @BeforeEach
  void writeGoldenFileItems() {
    GoldenFile.writeWithBatchWriteItem(client, TABLE_NAME, GoldenFile.readItems());
  }

  @Test
  void shouldFindCurrentAuthorMappingsThatAreNotDeleted() {
    assertThat(findEveryIdentifierInTheFileAndUnknownOnes().authorMappings())
        .isEqualTo(
            Map.of(
                AUTHOR_NEEDING_REVIEW, MIGRATED_AUTHOR_NEEDING_REVIEW,
                CORRECTED_AUTHOR, CURATOR_CORRECTED_AUTHOR));
  }

  @Test
  void shouldFindCurrentAffiliationMappingsAtInstitutionLevelAndSubUnit() {
    assertThat(findEveryIdentifierInTheFileAndUnknownOnes().affiliationMappings())
        .isEqualTo(
            Map.of(
                TOP_LEVEL_AFFILIATION, TOP_LEVEL_AFFILIATION_MAPPING,
                SUB_UNIT_AFFILIATION, SUB_UNIT_AFFILIATION_MAPPING));
  }

  @Test
  void shouldLeaveOutDeletedAndUnknownIdentifiers() {
    var mappings = findEveryIdentifierInTheFileAndUnknownOnes();

    assertThat(mappings.authorMapping(DELETED_AUTHOR)).isEmpty();
    assertThat(mappings.authorMapping(UNKNOWN_AUTHOR)).isEmpty();
    assertThat(mappings.affiliationMapping(UNKNOWN_AFFILIATION)).isEmpty();
  }

  @Test
  void shouldWriteCurrentItemOfCorrectionExactlyAsInTheFile() {
    var writtenItem = ScopusAuthorMappingDao.current(CURATOR_CORRECTED_AUTHOR).toItem();

    assertThat(writtenItem).isEqualTo(itemInTheFile(CORRECTED_AUTHOR_PARTITION_KEY, "CURRENT"));
  }

  @Test
  void shouldWriteHistoryItemOfCorrectionExactlyAsInTheFile() {
    var writtenItem =
        ScopusAuthorMappingDao.history(CURATOR_CORRECTED_AUTHOR, CORRECTION_CONTEXT).toItem();

    assertThat(writtenItem)
        .isEqualTo(
            itemInTheFile(CORRECTED_AUTHOR_PARTITION_KEY, "HISTORY#2026-11-20T13:45:00.000Z#2"));
  }

  private static Map<String, AttributeValue> itemInTheFile(String partitionKey, String sortKey) {
    return GoldenFile.readItems().stream()
        .filter(item -> partitionKey.equals(item.get(PRIMARY_KEY_PARTITION_KEY_NAME).s()))
        .filter(item -> sortKey.equals(item.get(PRIMARY_KEY_SORT_KEY_NAME).s()))
        .findFirst()
        .orElseThrow();
  }

  private ScopusMappings findEveryIdentifierInTheFileAndUnknownOnes() {
    return service.findCurrentMappings(
        List.of(AUTHOR_NEEDING_REVIEW, CORRECTED_AUTHOR, DELETED_AUTHOR, UNKNOWN_AUTHOR),
        List.of(TOP_LEVEL_AFFILIATION, SUB_UNIT_AFFILIATION, UNKNOWN_AFFILIATION));
  }
}
