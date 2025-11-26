package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.ResourceDao;

public class ResourceFilterMatcher implements EntityFilterMatcher {

  @Override
  public boolean matches(Dao dao, BatchFilter filter) {
    if (!(dao instanceof ResourceDao resourceDao)) {
      return false;
    }
    var resource = resourceDao.getResource();
    return matchesPublicationYear(resource, filter) && matchesStatus(resource, filter);
  }

  private boolean matchesPublicationYear(Resource resource, BatchFilter filter) {
    if (nonNull(filter.publicationYears()) && !filter.publicationYears().isEmpty()) {
      return extractYear(resource)
          .map(year -> filter.publicationYears().contains(year))
          .orElse(false);
    }
    if (nonNull(filter.publicationYear())) {
      return extractYear(resource).map(year -> year.equals(filter.publicationYear())).orElse(false);
    }
    return true;
  }

  private boolean matchesStatus(Resource resource, BatchFilter filter) {
    if (nonNull(filter.statuses()) && !filter.statuses().isEmpty()) {
      return Optional.ofNullable(resource.getStatus())
          .map(status -> filter.statuses().stream()
              .anyMatch(s -> s.equalsIgnoreCase(status.getValue())))
          .orElse(false);
    }
    if (isNull(filter.status())) {
      return true;
    }
    return Optional.ofNullable(resource.getStatus())
        .map(status -> status.getValue().equalsIgnoreCase(filter.status()))
        .orElse(false);
  }

  private Optional<String> extractYear(Resource resource) {
    return Optional.ofNullable(resource.getEntityDescription())
        .map(EntityDescription::getPublicationDate)
        .map(PublicationDate::getYear);
  }
}
