package no.unit.nva.publication.scopusmapping.domain;

import static java.util.Objects.requireNonNull;

import java.time.Instant;

/**
 * The mapping of a Scopus author to a Cristin person. {@code scopusName} and {@code orcid} describe
 * the Scopus author and may be null. {@code needsReview} is set by the PIA migration when PIA had
 * several candidate persons. {@code piaMappingCounts} is null unless the PIA migration wrote the
 * version.
 */
public record ScopusAuthorMapping(
    ScopusAuthorIdentifier scopusAuthorIdentifier,
    CristinPersonIdentifier cristinPersonIdentifier,
    String scopusName,
    String orcid,
    boolean needsReview,
    PiaMappingCounts piaMappingCounts,
    ScopusMappingVersion version)
    implements ScopusMapping {

  public ScopusAuthorMapping {
    requireNonNull(scopusAuthorIdentifier, "scopusAuthorIdentifier");
    requireNonNull(cristinPersonIdentifier, "cristinPersonIdentifier");
    requireNonNull(version, "version");
  }

  /** The first mapping of a Scopus author that had none, as a curator creates it. */
  public static ScopusAuthorMapping createdByCurator(
      ScopusAuthorIdentifier scopusAuthorIdentifier,
      CristinPersonIdentifier cristinPersonIdentifier,
      String username,
      Instant createdAt) {
    return new ScopusAuthorMapping(
        scopusAuthorIdentifier,
        cristinPersonIdentifier,
        null,
        null,
        false,
        null,
        ScopusMappingVersion.firstByCurator(username, createdAt));
  }

  /**
   * The next version of this mapping, as a curator's correction to the given person. It is the
   * curator's own: not flagged for review, and without the counts from PIA. It keeps what describes
   * the Scopus author. Correcting to the person the author is already mapped to is a confirmation,
   * and gives a new version like any other correction. Correcting a deleted mapping brings it back.
   */
  public ScopusAuthorMapping correctedTo(
      CristinPersonIdentifier correctCristinPersonIdentifier,
      String username,
      Instant correctedAt) {
    return new ScopusAuthorMapping(
        scopusAuthorIdentifier,
        correctCristinPersonIdentifier,
        scopusName,
        orcid,
        false,
        null,
        version.nextByCurator(username, correctedAt));
  }
}
