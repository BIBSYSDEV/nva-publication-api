package no.unit.nva.publication.events.handlers.batch.updates;

import java.net.URI;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.events.handlers.batch.Contributors;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.service.impl.ResourceService;

public final class ContributorAffiliationUpdate extends ResourceUpdate {

  public ContributorAffiliationUpdate(ResourceService resourceService) {
    super(resourceService);
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.CONTRIBUTOR_AFFILIATION;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var organization = organizationOf(request.oldValue());
    return Contributors.of(resource).stream()
        .flatMap(contributor -> contributor.affiliations().stream())
        .anyMatch(organization::equals);
  }

  @Override
  protected Resource update(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var updatedContributors =
        Contributors.of(resource).stream()
            .map(contributor -> withUpdatedAffiliations(contributor, request))
            .toList();

    Contributors.setOn(resource, updatedContributors);
    return resource;
  }

  private Contributor withUpdatedAffiliations(
      Contributor contributor, ManuallyUpdatePublicationsRequest request) {
    var updatedAffiliations =
        contributor.affiliations().stream()
            .map(corporation -> replaceIfMatching(corporation, request))
            .toList();

    return contributor.copy().withAffiliations(updatedAffiliations).build();
  }

  private Corporation replaceIfMatching(
      Corporation corporation, ManuallyUpdatePublicationsRequest request) {
    return corporation.equals(organizationOf(request.oldValue()))
        ? organizationOf(request.newValue())
        : corporation;
  }

  private Organization organizationOf(String organizationId) {
    return Organization.fromUri(URI.create(organizationId));
  }
}
