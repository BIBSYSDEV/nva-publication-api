package no.unit.nva.publication.scopusmapping;

import java.util.Map;
import java.util.Optional;

/**
 * The current mappings found for a set of Scopus ids, keyed on the Scopus id. Ids without a
 * mapping, or with a deleted one, are absent.
 */
public record ScopusMappings(
    Map<String, ScopusAuthorMapping> authorMappings,
    Map<String, ScopusAffiliationMapping> affiliationMappings) {

  public ScopusMappings {
    authorMappings = Map.copyOf(authorMappings);
    affiliationMappings = Map.copyOf(affiliationMappings);
  }

  public Optional<ScopusAuthorMapping> authorMapping(String scopusAuthorId) {
    return Optional.ofNullable(authorMappings.get(scopusAuthorId));
  }

  public Optional<ScopusAffiliationMapping> affiliationMapping(String scopusAffiliationId) {
    return Optional.ofNullable(affiliationMappings.get(scopusAffiliationId));
  }
}
