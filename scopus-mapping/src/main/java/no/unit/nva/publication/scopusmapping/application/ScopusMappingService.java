package no.unit.nva.publication.scopusmapping.application;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingRepository;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappings;

/** The use cases of Scopus mappings. */
public class ScopusMappingService {

  private final ScopusMappingRepository repository;

  public ScopusMappingService(ScopusMappingRepository repository) {
    this.repository = requireNonNull(repository, "repository");
  }

  /**
   * Looks up the current mappings of the Scopus authors and affiliations of a Scopus record. An
   * identifier may be given more than once. Identifiers without a mapping, or with a deleted one,
   * are absent from the result.
   */
  public ScopusMappings findCurrentMappings(
      Collection<ScopusAuthorIdentifier> scopusAuthorIdentifiers,
      Collection<ScopusAffiliationIdentifier> scopusAffiliationIdentifiers) {
    requireNoNulls(scopusAuthorIdentifiers, "scopusAuthorIdentifiers");
    requireNoNulls(scopusAffiliationIdentifiers, "scopusAffiliationIdentifiers");
    return repository.findCurrentMappings(scopusAuthorIdentifiers, scopusAffiliationIdentifiers);
  }

  private static void requireNoNulls(Collection<?> identifiers, String name) {
    requireNonNull(identifiers, name);
    identifiers.forEach(identifier -> requireNonNull(identifier, name + " must not contain null"));
  }
}
