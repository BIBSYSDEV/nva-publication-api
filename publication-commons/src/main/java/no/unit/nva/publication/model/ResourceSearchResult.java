package no.unit.nva.publication.model;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.publication.model.business.Resource;

public record ResourceSearchResult(
    int totalHits, int hitsReturned, List<Resource> resources, URI nextSearchAfterResults) {

  public Optional<URI> nextPage() {
    return hasHits() ? Optional.ofNullable(nextSearchAfterResults) : Optional.empty();
  }

  private boolean hasHits() {
    return hitsReturned > 0;
  }
}
