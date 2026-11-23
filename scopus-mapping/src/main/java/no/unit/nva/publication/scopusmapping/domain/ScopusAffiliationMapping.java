package no.unit.nva.publication.scopusmapping.domain;

import static java.util.Objects.requireNonNull;

/**
 * The mapping of a Scopus affiliation to a Cristin institution and a unit of that institution. The
 * unit is always set: a mapping at institution level has the institution itself as its unit. {@code
 * scopusName} may be null. {@code needsReview} is set by the PIA migration when PIA had candidates
 * in several institutions. {@code piaMappingCounts} is null unless the PIA migration wrote the
 * version.
 */
public record ScopusAffiliationMapping(
    ScopusAffiliationIdentifier scopusAffiliationIdentifier,
    CristinOrganizationIdentifier cristinInstitutionIdentifier,
    CristinOrganizationIdentifier cristinUnitIdentifier,
    String scopusName,
    boolean needsReview,
    PiaMappingCounts piaMappingCounts,
    ScopusMappingVersion version)
    implements ScopusMapping {

  public ScopusAffiliationMapping {
    requireNonNull(scopusAffiliationIdentifier, "scopusAffiliationIdentifier");
    requireNonNull(cristinInstitutionIdentifier, "cristinInstitutionIdentifier");
    requireNonNull(cristinUnitIdentifier, "cristinUnitIdentifier");
    requireNonNull(version, "version");
    if (!cristinInstitutionIdentifier.isTopLevel()) {
      throw new IllegalArgumentException(
          "Cristin institution %s of Scopus affiliation %s is not a top-level organization"
              .formatted(
                  cristinInstitutionIdentifier.value(), scopusAffiliationIdentifier.value()));
    }
    if (!cristinUnitIdentifier.topLevel().equals(cristinInstitutionIdentifier)) {
      throw new IllegalArgumentException(
          "Cristin unit %s of Scopus affiliation %s is not a unit of institution %s"
              .formatted(
                  cristinUnitIdentifier.value(),
                  scopusAffiliationIdentifier.value(),
                  cristinInstitutionIdentifier.value()));
    }
  }
}
