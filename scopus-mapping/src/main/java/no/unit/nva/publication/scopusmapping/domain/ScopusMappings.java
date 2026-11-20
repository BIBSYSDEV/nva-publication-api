package no.unit.nva.publication.scopusmapping.domain;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * The current mappings of a set of Scopus identifiers, such as those of one Scopus record. A
 * deleted mapping counts as no mapping, so identifiers without a mapping and identifiers with a
 * deleted one are both absent.
 */
public record ScopusMappings(
    Map<ScopusAuthorIdentifier, ScopusAuthorMapping> authorMappings,
    Map<ScopusAffiliationIdentifier, ScopusAffiliationMapping> affiliationMappings) {

  public ScopusMappings {
    authorMappings = withoutDeleted(authorMappings);
    affiliationMappings = withoutDeleted(affiliationMappings);
  }

  public static ScopusMappings of(
      Collection<ScopusAuthorMapping> authorMappings,
      Collection<ScopusAffiliationMapping> affiliationMappings) {
    return new ScopusMappings(
        authorMappings.stream()
            .collect(toUnmodifiableMap(ScopusAuthorMapping::scopusAuthorIdentifier, identity())),
        affiliationMappings.stream()
            .collect(
                toUnmodifiableMap(
                    ScopusAffiliationMapping::scopusAffiliationIdentifier, identity())));
  }

  public Optional<ScopusAuthorMapping> authorMapping(
      ScopusAuthorIdentifier scopusAuthorIdentifier) {
    return Optional.ofNullable(authorMappings.get(scopusAuthorIdentifier));
  }

  public Optional<ScopusAffiliationMapping> affiliationMapping(
      ScopusAffiliationIdentifier scopusAffiliationIdentifier) {
    return Optional.ofNullable(affiliationMappings.get(scopusAffiliationIdentifier));
  }

  private static <K, M extends ScopusMapping> Map<K, M> withoutDeleted(Map<K, M> mappings) {
    return mappings.entrySet().stream()
        .filter(mapping -> !mapping.getValue().version().deleted())
        .collect(toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
