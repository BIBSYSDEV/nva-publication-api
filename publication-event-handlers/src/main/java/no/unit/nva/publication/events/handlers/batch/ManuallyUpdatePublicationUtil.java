package no.unit.nva.publication.events.handlers.batch;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.unit.nva.publication.events.handlers.batch.updates.ContributorAffiliationUpdate;
import no.unit.nva.publication.events.handlers.batch.updates.ContributorIdentifierUpdate;
import no.unit.nva.publication.events.handlers.batch.updates.FileLicenseUpdate;
import no.unit.nva.publication.events.handlers.batch.updates.ProjectUpdate;
import no.unit.nva.publication.events.handlers.batch.updates.PublisherUpdate;
import no.unit.nva.publication.events.handlers.batch.updates.SerialPublicationUpdate;
import no.unit.nva.publication.events.handlers.batch.updates.UnconfirmedJournalUpdate;
import no.unit.nva.publication.events.handlers.batch.updates.UnconfirmedPublisherUpdate;
import no.unit.nva.publication.events.handlers.batch.updates.UnconfirmedSeriesUpdate;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

final class ManuallyUpdatePublicationUtil {

  private static final String NO_UPDATE_MESSAGE = "No manual update registered for type %s";
  private final Map<ManualUpdateType, ManualUpdate> updaters;

  private ManuallyUpdatePublicationUtil(Map<ManualUpdateType, ManualUpdate> updaters) {
    this.updaters = updaters;
  }

  static ManuallyUpdatePublicationUtil create(ResourceService resourceService) {
    return new ManuallyUpdatePublicationUtil(updatersByType(resourceService));
  }

  static Map<ManualUpdateType, ManualUpdate> updatersByType(ResourceService resourceService) {
    return allUpdaters(resourceService).stream()
        .collect(Collectors.toUnmodifiableMap(ManualUpdate::type, Function.identity()));
  }

  private static List<ManualUpdate> allUpdaters(ResourceService resourceService) {
    return List.of(
        new PublisherUpdate(resourceService),
        new SerialPublicationUpdate(resourceService),
        new UnconfirmedPublisherUpdate(resourceService),
        new UnconfirmedSeriesUpdate(resourceService),
        new UnconfirmedJournalUpdate(resourceService),
        new ContributorIdentifierUpdate(resourceService),
        new ContributorAffiliationUpdate(resourceService),
        new ProjectUpdate(resourceService),
        new FileLicenseUpdate(resourceService));
  }

  ManualUpdateResult update(List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
    var updater = updaterFor(request.type());
    var matchingResources =
        resources.stream().filter(resource -> updater.matches(resource, request)).toList();
    var plans =
        matchingResources.stream()
            .map(resource -> new UpdatePlan(resource, updater.plan(resource, request)))
            .toList();

    if (!request.isDryRun()) {
      plansWithChanges(plans).forEach(plan -> updater.commit(plan.resource(), request));
    }
    plans.forEach(plan -> UpdateLog.logPlan(request, plan));

    return new ManualUpdateResult(matchingResources.size(), changesOf(plans));
  }

  private static List<UpdatePlan> plansWithChanges(Collection<UpdatePlan> plans) {
    return plans.stream().filter(UpdatePlan::hasChanges).toList();
  }

  private static List<ResourceChange> changesOf(Collection<UpdatePlan> plans) {
    return plansWithChanges(plans).stream().map(UpdatePlan::toResourceChange).toList();
  }

  private ManualUpdate updaterFor(ManualUpdateType type) {
    return Optional.ofNullable(updaters.get(type))
        .orElseThrow(() -> new IllegalStateException(NO_UPDATE_MESSAGE.formatted(type)));
  }
}
