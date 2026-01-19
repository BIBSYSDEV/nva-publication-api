package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import java.util.Optional;
import no.unit.nva.model.ImportSource;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.publicationstate.FileImportedEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.FileDao;
import nva.commons.core.paths.UriWrapper;

public class FileFilterMatcher implements EntityFilterMatcher {

  @Override
  public boolean matches(Dao dao, BatchFilter filter) {
    if (!(dao instanceof FileDao fileDao)) {
      return false;
    }
    var fileEntry = fileDao.getFileEntry();
    return matchesImportSource(fileEntry, filter) && matchesOwnerAffiliation(fileEntry, filter);
  }

  private boolean matchesImportSource(FileEntry fileEntry, BatchFilter filter) {
    if (isNull(filter.fileImportSources()) || filter.fileImportSources().isEmpty()) {
      return true;
    }
    return extractImportSource(fileEntry)
        .filter(source -> filter.fileImportSources().contains(source))
        .isPresent();
  }

  private boolean matchesOwnerAffiliation(FileEntry fileEntry, BatchFilter filter) {
    if (isNull(filter.fileOwnerAffiliationPrefixes()) || filter.fileOwnerAffiliationPrefixes().isEmpty()) {
      return true;
    }
    return Optional.ofNullable(fileEntry.getOwnerAffiliation())
        .map(UriWrapper::fromUri)
        .map(UriWrapper::getLastPathElement)
        .filter(orgId -> filter.fileOwnerAffiliationPrefixes().stream().anyMatch(orgId::startsWith))
        .isPresent();
  }

  private Optional<String> extractImportSource(FileEntry fileEntry) {
    return Optional.ofNullable(fileEntry.getFileEvent())
        .filter(FileImportedEvent.class::isInstance)
        .map(FileImportedEvent.class::cast)
        .map(FileImportedEvent::importSource)
        .map(ImportSource::getSource)
        .map(ImportSource.Source::name);
  }
}
