package no.unit.nva.publication.scopusmapping.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Where mappings are kept. The domain and the use cases only know this interface. */
public interface ScopusMappingRepository {

  /**
   * Finds the current mappings of the given Scopus identifiers. Identifiers without a mapping are
   * absent from the result, and so are identifiers whose stored mapping can't be read.
   */
  ScopusMappings findCurrentMappings(
      Collection<ScopusAuthorIdentifier> scopusAuthorIdentifiers,
      Collection<ScopusAffiliationIdentifier> scopusAffiliationIdentifiers);

  /**
   * Finds the stored mapping of a Scopus author, deleted or not. Empty when nothing is stored.
   *
   * @throws IllegalStateException if something is stored that can't be read as a mapping
   */
  Optional<ScopusAuthorMapping> findAuthorMapping(ScopusAuthorIdentifier scopusAuthorIdentifier);

  /**
   * Saves a new version of an author mapping, with where it was corrected. The first version is
   * only saved if nothing is stored, and a later version only if the stored mapping is still at the
   * revision before it.
   *
   * @throws ScopusMappingConflictException if the stored mapping has changed
   */
  void saveCorrection(ScopusAuthorMapping newVersion, ScopusCorrectionContext correctionContext)
      throws ScopusMappingConflictException;

  /** Finds every written version of an author mapping, oldest first. */
  List<ScopusMappingHistoryEntry<ScopusAuthorMapping>> findAuthorMappingHistory(
      ScopusAuthorIdentifier scopusAuthorIdentifier);

  /** Finds every written version of an affiliation mapping, oldest first. */
  List<ScopusMappingHistoryEntry<ScopusAffiliationMapping>> findAffiliationMappingHistory(
      ScopusAffiliationIdentifier scopusAffiliationIdentifier);
}
