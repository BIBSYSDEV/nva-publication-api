package no.unit.nva.publication.events.handlers.batch;

import java.util.Collection;
import java.util.List;
import no.unit.nva.commons.json.JsonSerializable;

public record ManuallyUpdatePublicationsReport(
    boolean dryRun,
    int limit,
    boolean limitReached,
    int pageSize,
    ManualUpdateType type,
    String oldValue,
    String newValue,
    int totalHits,
    int pagesFetched,
    int hitsReturned,
    int resourcesFetched,
    int resourcesMatched,
    int resourcesChanged,
    List<ResourceChange> changes)
    implements JsonSerializable {

  public static ManuallyUpdatePublicationsReport create(
      ManuallyUpdatePublicationsRequest request, ManualUpdateProgress progress) {
    return create(request, progress, progress.changes());
  }

  public static ManuallyUpdatePublicationsReport summary(
      ManuallyUpdatePublicationsRequest request, ManualUpdateProgress progress) {
    return create(request, progress, List.of());
  }

  private static ManuallyUpdatePublicationsReport create(
      ManuallyUpdatePublicationsRequest request,
      ManualUpdateProgress progress,
      Collection<ResourceChange> changes) {
    return new ManuallyUpdatePublicationsReport(
        request.isDryRun(),
        request.maxChanges(),
        progress.limitReached(request.maxChanges()),
        request.searchPageSize(),
        request.type(),
        request.oldValue(),
        request.newValue(),
        progress.totalHits(),
        progress.pagesFetched(),
        progress.hitsReturned(),
        progress.resourcesFetched(),
        progress.resourcesMatched(),
        progress.resourcesChanged(),
        List.copyOf(changes));
  }
}
