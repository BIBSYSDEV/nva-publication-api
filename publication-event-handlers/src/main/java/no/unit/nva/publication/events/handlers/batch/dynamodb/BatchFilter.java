package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import java.util.Collection;
import no.unit.nva.commons.json.JsonSerializable;
import nva.commons.core.StringUtils;

public record BatchFilter(
    String publicationYear,
    Collection<String> publicationYears,
    String status,
    Collection<String> statuses)
    implements JsonSerializable {

  public boolean isEmpty() {
    return StringUtils.isNotBlank(publicationYear)
           && (isNull(publicationYears) || publicationYears.isEmpty())
           && StringUtils.isNotBlank(status)
           && (isNull(statuses) || statuses.isEmpty());
  }
}
