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
    List<ResourceChange> changes)
    implements JsonSerializable {

  public static ManuallyUpdatePublicationsReport create(
      ManuallyUpdatePublicationsRequest request,
      ResourceSearchResult searchResult,
      List<ResourceChange> changes) {
    return new ManuallyUpdatePublicationsReport(
        request.dryRun(),
        request.type(),
        request.oldValue(),
        request.newValue(),
        searchResult.totalHits(),
        searchResult.hitsReturned(),
        searchResult.resources().size(),
        changes.size(),
        changes);
  }
}
