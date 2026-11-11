package no.unit.nva.publication.scopusmapping.domain;

/** What the mapping of a Scopus author and the mapping of a Scopus affiliation have in common. */
public sealed interface ScopusMapping permits ScopusAuthorMapping, ScopusAffiliationMapping {

  ScopusMappingVersion version();
}
