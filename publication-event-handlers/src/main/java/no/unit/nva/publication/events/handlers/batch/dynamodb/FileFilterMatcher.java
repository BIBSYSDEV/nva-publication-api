package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static java.util.Objects.isNull;

import java.util.Optional;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.publicationstate.FileImportedEvent;
import no.unit.nva.publication.model.storage.Dao;
import no.unit.nva.publication.model.storage.FileDao;

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
    var sourceFromEvent = extractImportSourceFromEvent(fileEntry);
    var sourceFromUploadDetails = extractImportSourceFromUploadDetails(fileEntry);

    return sourceFromEvent.filter(source -> filter.fileImportSources().contains(source)).isPresent()
        || sourceFromUploadDetails
            .filter(source -> filter.fileImportSources().contains(source))
            .isPresent();
  }

  private boolean matchesOwnerAffiliation(FileEntry fileEntry, BatchFilter filter) {
    if (isNull(filter.fileOwnerAffiliationContains()) || filter.fileOwnerAffiliationContains().isEmpty()) {
      return true;
    }
    return Optional.ofNullable(fileEntry.getOwnerAffiliation())
        .map(Object::toString)
        .filter(
            affiliation ->
                filter.fileOwnerAffiliationContains().stream().anyMatch(affiliation::contains))
        .isPresent();
  }

  private Optional<String> extractImportSourceFromEvent(FileEntry fileEntry) {
    return Optional.ofNullable(fileEntry.getFileEvent())
        .filter(FileImportedEvent.class::isInstance)
        .map(FileImportedEvent.class::cast)
        .map(FileImportedEvent::importSource)
        .map(ImportSource::getSource)
        .map(ImportSource.Source::name);
  }

  private Optional<String> extractImportSourceFromUploadDetails(FileEntry fileEntry) {
    return Optional.ofNullable(fileEntry.getFile())
        .map(File::getUploadDetails)
        .filter(ImportUploadDetails.class::isInstance)
        .map(ImportUploadDetails.class::cast)
        .map(ImportUploadDetails::source)
        .map(ImportUploadDetails.Source::name);
  }
}
