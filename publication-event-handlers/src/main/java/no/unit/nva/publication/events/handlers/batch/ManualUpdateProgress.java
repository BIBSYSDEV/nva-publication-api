package no.unit.nva.publication.events.handlers.batch;

import java.util.List;
import java.util.stream.Stream;
import no.unit.nva.publication.model.ResourceSearchResult;

record ManualUpdateProgress(
    int totalHits,
    int pagesFetched,
    int hitsReturned,
    int resourcesFetched,
    int resourcesMatched,
    List<ResourceChange> changes) {

  static ManualUpdateProgress empty() {
    return new ManualUpdateProgress(0, 0, 0, 0, 0, List.of());
  }

  ManualUpdateProgress plus(ResourceSearchResult page, ManualUpdateResult result) {
    return new ManualUpdateProgress(
        totalHitsReportedByFirstPage(page),
        pagesFetched + 1,
        hitsReturned + page.hitsReturned(),
        resourcesFetched + page.resources().size(),
        resourcesMatched + result.matchedResources(),
        Stream.concat(changes.stream(), result.changes().stream()).toList());
  }

  int resourcesChanged() {
    return changes.size();
  }

  int remainingChanges(int maxChanges) {
    return maxChanges - resourcesChanged();
  }

  boolean limitReached(int maxChanges) {
    return remainingChanges(maxChanges) <= 0;
  }

  private int totalHitsReportedByFirstPage(ResourceSearchResult page) {
    return pagesFetched == 0 ? page.totalHits() : totalHits;
  }
}
