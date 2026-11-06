package no.unit.nva.publication.scopusmapping;

import static no.unit.nva.publication.scopusmapping.ScopusMappingItems.toAuthorMapping;

import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** The stored {@code CURRENT} item of an author mapping, as the write path sees it. */
record StoredAuthorMapping(Optional<ScopusAuthorMapping> mapping, Optional<Integer> revision) {

  static final StoredAuthorMapping NONE =
      new StoredAuthorMapping(Optional.empty(), Optional.empty());

  static StoredAuthorMapping fromItem(Map<String, AttributeValue> item) {
    var storedMapping = toAuthorMapping(item);
    return new StoredAuthorMapping(
        Optional.of(storedMapping), Optional.of(storedMapping.revision()));
  }

  /** An empty expected revision means the caller saw no mapping: there must be no live one. */
  boolean hasExpectedRevision(Optional<Integer> expectedRevision) {
    return expectedRevision.isPresent()
        ? revision.equals(expectedRevision)
        : liveMapping().isEmpty();
  }

  Optional<ScopusAuthorMapping> liveMappingTo(String cristinPersonId) {
    return liveMapping()
        .filter(storedMapping -> storedMapping.cristinPersonId().equals(cristinPersonId));
  }

  private Optional<ScopusAuthorMapping> liveMapping() {
    return mapping.filter(storedMapping -> !storedMapping.deleted());
  }
}
