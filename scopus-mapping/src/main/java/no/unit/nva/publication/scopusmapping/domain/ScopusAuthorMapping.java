package no.unit.nva.publication.scopusmapping.domain;

import static java.util.Objects.requireNonNull;

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
}
