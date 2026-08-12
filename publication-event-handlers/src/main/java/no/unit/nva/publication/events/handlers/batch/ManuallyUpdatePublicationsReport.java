package no.unit.nva.publication.events.handlers.batch;

import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.publication.model.ResourceSearchResult;

public record ManuallyUpdatePublicationsReport(
    boolean dryRun,
    ManualUpdateType type,
    String oldValue,
    String newValue,
    int totalHits,
    int hitsReturned,
    int resourcesFetched,
    int resourcesMatched,
    int resourcesChanged,
    List<ResourceChange> changes)
    implements JsonSerializable {

  public static ManuallyUpdatePublicationsReport create(
      ManuallyUpdatePublicationsRequest request,
      ResourceSearchResult searchResult,
      ManualUpdateResult updateResult) {
    return new ManuallyUpdatePublicationsReport(
        request.isDryRun(),
        request.type(),
        request.oldValue(),
        request.newValue(),
        searchResult.totalHits(),
        searchResult.hitsReturned(),
        searchResult.resources().size(),
        updateResult.matchedResources(),
        updateResult.changes().size(),
        updateResult.changes());
  }
}
