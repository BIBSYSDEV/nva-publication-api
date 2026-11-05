package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.ScopusMappingSource.PIA_MIGRATION;

import java.time.Instant;

/** Mappings and DynamoDB items shared by the Scopus mapping tests. */
final class ScopusMappingFixtures {

  static final String SCOPUS_AUTHOR_ID = "99999990201";
  static final String MIGRATED_CRISTIN_PERSON_ID = "1001";
  static final Instant MIGRATED_AT = Instant.parse("2026-11-02T08:00:00.000Z");
  static final String MIGRATION_MARKER = "pia-migration";

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
}
