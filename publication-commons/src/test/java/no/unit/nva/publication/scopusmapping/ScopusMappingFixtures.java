package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.CURATOR;
import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.PIA_MIGRATION;

import java.time.Instant;

/** Mappings and DynamoDB items shared by the Scopus mapping tests. */
final class ScopusMappingFixtures {

  static final String FIXTURES = "no.unit.nva.publication.scopusmapping.ScopusMappingFixtures";
  static final String TABLE_NAME = "import-candidates";
  static final String SCOPUS_AUTHOR_ID = "99999990201";
  static final String MIGRATED_CRISTIN_PERSON_ID = "1001";
  static final Instant MIGRATED_AT = Instant.parse("2026-11-02T08:00:00.000Z");
  static final String MIGRATION_MARKER = "pia-migration";
  static final String CURATOR_USERNAME = "1234567@20754.0.0.0";

  private ScopusMappingFixtures() {}

  static ScopusAuthorMapping migratedAuthorMapping(String scopusAuthorId) {
    return new ScopusAuthorMapping(
        scopusAuthorId,
        MIGRATED_CRISTIN_PERSON_ID,
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
  }

  /** A curator's version of a mapping, with none of the fields that only the migration sets. */
  static ScopusAuthorMapping curatorAuthorMapping(
      String scopusAuthorId,
      String cristinPersonId,
      Instant updatedAt,
      int revision,
      boolean deleted) {
    return new ScopusAuthorMapping(
        scopusAuthorId,
        cristinPersonId,
        null,
        null,
        null,
        null,
        false,
        CURATOR,
        updatedAt,
        CURATOR_USERNAME,
        revision,
        deleted);
  }
}
