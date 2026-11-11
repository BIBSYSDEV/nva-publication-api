package no.unit.nva.publication.scopusmapping.application;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import no.unit.nva.publication.scopusmapping.domain.CristinPersonIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAffiliationMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorIdentifier;
import no.unit.nva.publication.scopusmapping.domain.ScopusAuthorMapping;
import no.unit.nva.publication.scopusmapping.domain.ScopusCorrectionContext;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingConflictException;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingHistoryEntry;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappingRepository;
import no.unit.nva.publication.scopusmapping.domain.ScopusMappings;

/** The use cases of Scopus mappings. */
public class ScopusMappingService {

  private static final String NO_EXPECTED_REVISION = "none";

  private final ScopusMappingRepository repository;
  private final Clock clock;

  public ScopusMappingService(ScopusMappingRepository repository, Clock clock) {
    this.repository = requireNonNull(repository, "repository");
    this.clock = requireNonNull(clock, "clock");
  }

  /**
   * Maps a Scopus author to a Cristin person, as a curator's correction. The stored mapping must
   * have {@code expectedRevision}, or, when it is empty, there must be no mapping (a deleted
   * mapping counts as none). Correcting to the person the author is already mapped to is a
   * confirmation: it saves a new version like any other correction, which clears the review flag.
   *
   * @throws ScopusMappingConflictException if the stored mapping does not have the expected
   *     revision, or changes before the correction is saved
   * @throws IllegalStateException if what is stored for the author can't be read as a mapping. That
   *     is a fault in the stored data, which has to be repaired before the author can be corrected.
   */
  public ScopusAuthorMapping correctAuthorMapping(
      ScopusAuthorIdentifier scopusAuthorIdentifier,
      CristinPersonIdentifier cristinPersonIdentifier,
      ScopusCorrectionContext correctionContext,
      String username,
      Optional<Integer> expectedRevision)
      throws ScopusMappingConflictException {
    requireNonNull(scopusAuthorIdentifier, "scopusAuthorIdentifier");
    requireNonNull(cristinPersonIdentifier, "cristinPersonIdentifier");
    requireNonNull(correctionContext, "correctionContext");
    requireNonNull(username, "username");
    requireNonNull(expectedRevision, "expectedRevision");
    var storedMapping = repository.findAuthorMapping(scopusAuthorIdentifier);
    if (!isWhatTheCallerExpects(storedMapping, expectedRevision)) {
      throw new ScopusMappingConflictException(
          "Scopus author mapping %s is not at the expected revision (%s)"
              .formatted(
                  scopusAuthorIdentifier.value(),
                  expectedRevision.map(String::valueOf).orElse(NO_EXPECTED_REVISION)));
    }
    var correctedAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
    var newVersion =
        storedMapping
            .map(stored -> stored.correctedTo(cristinPersonIdentifier, username, correctedAt))
            .orElseGet(
                () ->
                    ScopusAuthorMapping.createdByCurator(
                        scopusAuthorIdentifier, cristinPersonIdentifier, username, correctedAt));
    repository.saveCorrection(newVersion, correctionContext);
    return newVersion;
  }

  /**
   * A caller that expects a revision must name the stored one. A caller that expects none saw no
   * mapping, so there must be no live one: a deleted mapping counts as none.
   */
  private static boolean isWhatTheCallerExpects(
      Optional<ScopusAuthorMapping> storedMapping, Optional<Integer> expectedRevision) {
    return expectedRevision
        .map(
            expected ->
                storedMapping.filter(stored -> stored.version().revision() == expected).isPresent())
        .orElseGet(() -> storedMapping.filter(stored -> !stored.version().deleted()).isEmpty());
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

  /** Reads every written version of an author mapping, oldest first. */
  public List<ScopusMappingHistoryEntry<ScopusAuthorMapping>> findAuthorMappingHistory(
      ScopusAuthorIdentifier scopusAuthorIdentifier) {
    requireNonNull(scopusAuthorIdentifier, "scopusAuthorIdentifier");
    return repository.findAuthorMappingHistory(scopusAuthorIdentifier);
  }

  /** Reads every written version of an affiliation mapping, oldest first. */
  public List<ScopusMappingHistoryEntry<ScopusAffiliationMapping>> findAffiliationMappingHistory(
      ScopusAffiliationIdentifier scopusAffiliationIdentifier) {
    requireNonNull(scopusAffiliationIdentifier, "scopusAffiliationIdentifier");
    return repository.findAffiliationMappingHistory(scopusAffiliationIdentifier);
  }

  private static void requireNoNulls(Collection<?> identifiers, String name) {
    requireNonNull(identifiers, name);
    identifiers.forEach(identifier -> requireNonNull(identifier, name + " must not contain null"));
  }
}
