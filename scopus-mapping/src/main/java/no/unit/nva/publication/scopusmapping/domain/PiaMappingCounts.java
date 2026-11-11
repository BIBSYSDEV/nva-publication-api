package no.unit.nva.publication.scopusmapping.domain;

/**
 * How well founded a migrated mapping was in PIA: {@code mappingCount} of the {@code
 * totalMappingCount} PIA rows for the Scopus identifier pointed to the chosen Cristin identifier,
 * so "5 of 7" is a contested mapping. Only the version written by the PIA migration has counts.
 */
public record PiaMappingCounts(Integer mappingCount, Integer totalMappingCount) {}
