package no.unit.nva.publication.permissions.file.grant;

import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_ALL;

import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.FileStatus;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FileGrantStrategy;
import no.unit.nva.publication.permissions.file.FileStrategyBase;

public class EditorFileGrantStrategy extends FileStrategyBase implements FileGrantStrategy {

  public EditorFileGrantStrategy(FileEntry file, UserInstance userInstance, Resource resource) {
    super(file, userInstance, resource);
  }

  @Override
  public boolean allowsAction(FileOperation permission) {
    if (hasAccessRight(MANAGE_RESOURCES_ALL)) {
      return switch (permission) {
        case READ_METADATA, DOWNLOAD -> userRelatesToPublication() || isAllowedFileType();
        case WRITE_METADATA, DELETE -> false;
      };
    }

    return false;
  }

  private boolean isAllowedFileType() {
    var status = FileStatus.from(file.getFile());
    return !(status == FileStatus.PENDING_INTERNAL
        || status == FileStatus.PENDING_OPEN && file.getFile().hasActiveEmbargo());
  }
}
