package no.unit.nva.publication.permissions.file.grant;

import static no.unit.nva.model.PublicationStatus.PUBLISHED;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.FileStatus;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public final class EveryoneGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

  public EveryoneGrantStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
    super(file, userInstance, resource);
  }

  @Override
  public boolean allowsAction(FileOperation permission) {
    if (FileStatus.from(file.getFile()) == FileStatus.OPEN) {
      return PUBLISHED.equals(resource.getStatus())
          && switch (permission) {
            case READ_METADATA -> true;
            case WRITE_METADATA, DELETE -> false;
            case DOWNLOAD -> !file.getFile().hasActiveEmbargo();
          };
    }

    return false;
  }
}
