package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import java.util.Optional;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.ResourceDao;

public class ResourceFilterMatcher implements EntityFilterMatcher {

  @Override
  public boolean matches(Dao dao, BatchFilter filter) {
    if (dao instanceof ResourceDao resourceDao) {
      var resource = resourceDao.getResource();
      return matchesPublicationYears(resource, filter) && matchesStatuses(resource, filter);
    }
    return false;
  }

  private boolean matchesPublicationYears(Resource resource, BatchFilter filter) {
    if (isNull(filter.publicationYears()) || filter.publicationYears().isEmpty()) {
      return true;
    }
    return extractYear(resource).filter(filter.publicationYears()::contains).isPresent();
  }

  private boolean matchesStatuses(Resource resource, BatchFilter filter) {
    if (isNull(filter.statuses()) || filter.statuses().isEmpty()) {
      return true;
    }
    return Optional.ofNullable(resource.getStatus())
        .map(status -> filter.statuses().stream()
            .anyMatch(filterStatus -> filterStatus.equalsIgnoreCase(status.toString())))
        .orElse(false);
  }

  private Optional<String> extractYear(Resource resource) {
    return Optional.ofNullable(resource.getEntityDescription())
        .map(EntityDescription::getPublicationDate)
        .map(PublicationDate::getYear);
  }
}
