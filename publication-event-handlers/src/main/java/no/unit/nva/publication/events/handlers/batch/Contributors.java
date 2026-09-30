package no.unit.nva.publication.events.handlers.batch;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Identity;
import no.unit.nva.publication.model.business.Resource;
import nva.commons.core.paths.UriWrapper;

public final class Contributors {

  private static final String CRISTIN_PATH = "cristin";
  private static final String PERSON_PATH = "person";

  private Contributors() {}

  public static List<Contributor> of(Resource resource) {
    return Optional.ofNullable(resource.getEntityDescription())
        .map(EntityDescription::getContributors)
        .orElseGet(Collections::emptyList);
  }

  public static void setOn(Resource resource, List<Contributor> contributors) {
    resource.getEntityDescription().setContributors(contributors);
  }

  public static URI personUri(String personIdentifier) {
    return UriWrapper.fromHost(API_HOST)
        .addChild(CRISTIN_PATH, PERSON_PATH, personIdentifier)
        .getUri();
  }

  public static boolean hasIdentifier(Contributor contributor, String contributorIdentifier) {
    return Optional.ofNullable(contributor)
        .map(Contributor::identity)
        .map(Identity::getId)
        .map(UriWrapper::fromUri)
        .map(UriWrapper::getLastPathElement)
        .filter(contributorIdentifier::equals)
        .isPresent();
  }
}
