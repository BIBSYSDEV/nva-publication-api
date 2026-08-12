package no.unit.nva.publication.events.handlers.batch;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.core.paths.UriWrapper;

final class ContributorUpdater {

  private static final String CRISTIN = "cristin";
  private static final String PERSON = "person";
  private final ApiUriProvider uriProvider;

  private ContributorUpdater(ApiUriProvider uriProvider) {
    this.uriProvider = uriProvider;
  }

  static ContributorUpdater create(ApiUriProvider uriProvider) {
    return new ContributorUpdater(uriProvider);
  }

  boolean hasContributor(Resource resource, String contributorId) {
    return Optional.ofNullable(resource.getEntityDescription())
        .map(EntityDescription::getContributors)
        .stream()
        .flatMap(List::stream)
        .anyMatch(contributor -> hasIdentifier(contributor, contributorId));
  }

  Resource updateIdentifier(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var contributors = new ArrayList<>(resource.getEntityDescription().getContributors());
    var contributorToUpdate =
        contributors.stream()
            .filter(contributor -> hasIdentifier(contributor, request.oldValue()))
            .findFirst()
            .orElseThrow();

    contributors.remove(contributorToUpdate);
    contributorToUpdate.identity().setId(uriProvider.uriFrom(CRISTIN, PERSON, request.newValue()));
    contributors.add(contributorToUpdate);
    resource.getEntityDescription().setContributors(contributors);
    return resource;
  }

  boolean hasOrganization(Resource resource, String organizationId) {
    var organization = Organization.fromUri(URI.create(organizationId));
    return resource.getEntityDescription().getContributors().stream()
        .anyMatch(contributor -> hasAffiliation(contributor, organization));
  }

  Resource updateAffiliation(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var updatedContributors =
        resource.getEntityDescription().getContributors().stream()
            .map(contributor -> updateContributor(contributor, request))
            .toList();

    resource.getEntityDescription().setContributors(updatedContributors);
    return resource;
  }

  private Contributor updateContributor(
      Contributor contributor, ManuallyUpdatePublicationsRequest request) {
    var updatedAffiliations =
        contributor.affiliations().stream()
            .map(corporation -> updateCorporation(corporation, request))
            .toList();

    return contributor.copy().withAffiliations(updatedAffiliations).build();
  }

  private Corporation updateCorporation(
      Corporation corporation, ManuallyUpdatePublicationsRequest request) {
    var organizationToReplace = Organization.fromUri(URI.create(request.oldValue()));
    if (corporation instanceof Organization organization
        && organization.equals(organizationToReplace)) {
      return Organization.fromUri(URI.create(request.newValue()));
    }
    return corporation;
  }

  private boolean hasAffiliation(Contributor contributor, Organization organization) {
    return contributor.affiliations().stream().anyMatch(organization::equals);
  }

  private boolean hasIdentifier(Contributor contributor, String contributorIdentifier) {
    return Optional.ofNullable(contributor)
        .map(Contributor::identity)
        .map(Identity::getId)
        .map(UriWrapper::fromUri)
        .map(UriWrapper::getLastPathElement)
        .filter(contributorIdentifier::equals)
        .isPresent();
  }
}
