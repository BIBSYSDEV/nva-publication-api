package no.unit.nva.publication.scopusmapping.domain;

import java.util.Collection;
import java.util.List;

/** Where mappings are kept. The domain and the use cases only know this interface. */
public interface ScopusMappingRepository {

  /**
   * Finds the current mappings of the given Scopus identifiers. Identifiers without a mapping are
   * absent from the result, and so are identifiers whose stored mapping can't be read.
   */
  ScopusMappings findCurrentMappings(
      Collection<ScopusAuthorIdentifier> scopusAuthorIdentifiers,
      Collection<ScopusAffiliationIdentifier> scopusAffiliationIdentifiers);

  /** Finds every written version of an author mapping, oldest first. */
  List<ScopusMappingHistoryEntry<ScopusAuthorMapping>> findAuthorMappingHistory(
      ScopusAuthorIdentifier scopusAuthorIdentifier);

  /** Finds every written version of an affiliation mapping, oldest first. */
  List<ScopusMappingHistoryEntry<ScopusAffiliationMapping>> findAffiliationMappingHistory(
      ScopusAffiliationIdentifier scopusAffiliationIdentifier);
}
