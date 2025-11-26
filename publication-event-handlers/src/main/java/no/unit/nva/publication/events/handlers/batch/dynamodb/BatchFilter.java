package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import java.util.Collection;
import no.unit.nva.commons.json.JsonSerializable;

public record BatchFilter(
    String publicationYear,
    Collection<String> publicationYears,
    String status,
    Collection<String> statuses)
    implements JsonSerializable {

  public boolean isEmpty() {
    return isNull(publicationYear)
        && (isNull(publicationYears) || publicationYears.isEmpty())
        && isNull(status)
        && (isNull(statuses) || statuses.isEmpty());
  }
}
