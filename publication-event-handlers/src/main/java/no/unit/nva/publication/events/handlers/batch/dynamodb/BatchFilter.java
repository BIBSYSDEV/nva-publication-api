package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import java.util.Collection;
import no.unit.nva.commons.json.JsonSerializable;

public record BatchFilter(
    Collection<String> publicationYears,
    Collection<String> statuses,
    Collection<String> fileImportSources,
    Collection<String> fileOwnerAffiliationContains)
    implements JsonSerializable {

  public boolean isEmpty() {
    return (isNull(publicationYears) || publicationYears.isEmpty())
        && (isNull(statuses) || statuses.isEmpty())
        && (isNull(fileImportSources) || fileImportSources.isEmpty())
        && (isNull(fileOwnerAffiliationContains) || fileOwnerAffiliationContains.isEmpty());
  }
}
