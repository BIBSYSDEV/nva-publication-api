package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.domain.ScopusMappingSource.PIA_MIGRATION;

import java.time.Instant;
import no.unit.nva.publication.scopusmapping.domain.CristinOrganizationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.CristinPersonIdentifier;
import no.unit.nva.publication.scopusmapping.domain.PiaMappingCounts;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingVersion;

/** Mappings shared by the Scopus mapping tests. */
public final class ScopusMappingFixtures {

  public static final ScopusAuthorIdentifier SCOPUS_AUTHOR =
      new ScopusAuthorIdentifier("99999990201");
  public static final ScopusAffiliationIdentifier SCOPUS_AFFILIATION =
      new ScopusAffiliationIdentifier("99990201");
  public static final CristinPersonIdentifier MIGRATED_PERSON = new CristinPersonIdentifier("1001");
  public static final Instant MIGRATED_AT = Instant.parse("2026-11-02T08:00:00.000Z");
  public static final String MIGRATION_MARKER = "pia-migration";

  private ScopusMappingFixtures() {}

  public static ScopusMappingVersion migratedVersion() {
    return new ScopusMappingVersion(PIA_MIGRATION, MIGRATED_AT, MIGRATION_MARKER, 1, false);
  }

  public static ScopusAuthorMapping migratedAuthorMapping(
      ScopusAuthorIdentifier scopusAuthorIdentifier) {
    return new ScopusAuthorMapping(
        scopusAuthorIdentifier,
        MIGRATED_PERSON,
        "Nordmann, Ola",
        "0000-0002-1825-0097",
        true,
        new PiaMappingCounts(5, 7),
        migratedVersion());
  }

  public static ScopusAffiliationMapping migratedAffiliationMapping(
      String cristinInstitutionIdentifier, String cristinUnitIdentifier) {
    return new ScopusAffiliationMapping(
        SCOPUS_AFFILIATION,
        new CristinOrganizationIdentifier(cristinInstitutionIdentifier),
        new CristinOrganizationIdentifier(cristinUnitIdentifier),
        null,
        false,
        null,
        migratedVersion());
  }
}
