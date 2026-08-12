package no.unit.nva.publication.events.handlers.batch;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ManuallyUpdatePublicationUtil {

  private static final Logger logger = LoggerFactory.getLogger(ManuallyUpdatePublicationUtil.class);
  private static final String DRY_RUN_PREFIX = "DRY RUN: would update";
  private static final String UPDATE_PREFIX = "Updating";
  private final ResourceService resourceService;
  private final PublicationChannelUpdater channelUpdater;
  private final ContributorUpdater contributorUpdater;
  private final ProjectUpdater projectUpdater;
  private final FileLicenseUpdater licenseUpdater;

  private ManuallyUpdatePublicationUtil(ResourceService resourceService, Environment environment) {
    var uriProvider = ApiUriProvider.create(environment);
    this.resourceService = resourceService;
    this.channelUpdater = PublicationChannelUpdater.create(uriProvider);
    this.contributorUpdater = ContributorUpdater.create(uriProvider);
    this.projectUpdater = ProjectUpdater.create(uriProvider);
    this.licenseUpdater = FileLicenseUpdater.create(resourceService);
  }

  public static ManuallyUpdatePublicationUtil create(
      ResourceService resourceService, Environment environment) {
    return new ManuallyUpdatePublicationUtil(resourceService, environment);
  }

  public List<ResourceChange> update(
      List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
    return switch (request.type()) {
      case PUBLISHER ->
          updateResources(
              resources, request, channelUpdater::hasPublisher, channelUpdater::updatePublisher);
      case SERIAL_PUBLICATION ->
          updateResources(
              resources,
              request,
              channelUpdater::hasSerialPublication,
              channelUpdater::updateSeriesOrJournal);
      case LICENSE -> updateLicenses(resources, request);
      case UNCONFIRMED_PUBLISHER ->
          updateResources(
              resources,
              request,
              comparingFilter(request, channelUpdater::hasUnconfirmedPublisher),
              channelUpdater::updateUnconfirmedPublisher);
      case UNCONFIRMED_SERIES ->
          updateResources(
              resources,
              request,
              comparingFilter(request, channelUpdater::hasUnconfirmedSeries),
              channelUpdater::updateUnconfirmedSeries);
      case UNCONFIRMED_JOURNAL ->
          updateResources(
              resources,
              request,
              comparingFilter(request, channelUpdater::hasUnconfirmedJournal),
              channelUpdater::updateUnconfirmedJournal);
      case CONTRIBUTOR_IDENTIFIER ->
          updateResources(
              resources,
              request,
              contributorUpdater::hasContributor,
              contributorUpdater::updateIdentifier);
      case CONTRIBUTOR_AFFILIATION ->
          updateResources(
              resources,
              request,
              contributorUpdater::hasOrganization,
              contributorUpdater::updateAffiliation);
      case PROJECT ->
          updateResources(
              resources, request, projectUpdater::hasProject, projectUpdater::updateProject);
    };
  }

  private static BiPredicate<Resource, String> comparingFilter(
      ManuallyUpdatePublicationsRequest request, ComparingFilter filter) {
    return (resource, value) -> filter.test(resource, value, request.comparator());
  }

  private List<ResourceChange> updateResources(
      List<Resource> resources,
      ManuallyUpdatePublicationsRequest request,
      BiPredicate<Resource, String> filter,
      BiFunction<Resource, ManuallyUpdatePublicationsRequest, Resource> updater) {
    var matchingResources =
        resources.stream().filter(resource -> filter.test(resource, request.oldValue())).toList();
    var changes =
        matchingResources.stream()
            .map(resource -> applyAndDescribe(resource, request, updater))
            .filter(ResourceChange::hasChanges)
            .toList();

    logUpdate(request, matchingResources.size());
    if (request.dryRun()) {
      return changes;
    }
    matchingResources.forEach(this::persist);
    return changes;
  }

  private List<ResourceChange> updateLicenses(
      List<Resource> resources, ManuallyUpdatePublicationsRequest request) {
    var changes =
        resources.stream()
            .map(resource -> licenseUpdater.updateLicense(resource, request))
            .filter(ResourceChange::hasChanges)
            .toList();

    logUpdate(request, changes.size());
    return changes;
  }

  private ResourceChange applyAndDescribe(
      Resource resource,
      ManuallyUpdatePublicationsRequest request,
      BiFunction<Resource, ManuallyUpdatePublicationsRequest, Resource> updater) {
    var before = PublicationDiff.snapshot(resource.toPublication());
    var target = request.dryRun() ? copyOf(resource) : resource;
    var after = PublicationDiff.snapshot(updater.apply(target, request).toPublication());

    return new ResourceChange(
        resource.getIdentifier().toString(), PublicationDiff.between(before, after));
  }

  private Resource copyOf(Resource resource) {
    return Resource.fromPublication(resource.toPublication());
  }

  private void persist(Resource resource) {
    resourceService.updateResource(
        resource, UserInstance.fromPublication(resource.toPublication()));
  }

  private void logUpdate(ManuallyUpdatePublicationsRequest request, int resourceCount) {
    logger.info(
        "{} {} from {} to {} for {} resources",
        request.dryRun() ? DRY_RUN_PREFIX : UPDATE_PREFIX,
        request.type(),
        request.oldValue(),
        request.newValue(),
        resourceCount);
  }

  @FunctionalInterface
  private interface ComparingFilter {
    boolean test(Resource resource, String value, Comparator comparator);
  }
}
