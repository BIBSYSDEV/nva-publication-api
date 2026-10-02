package no.unit.nva.publication.model;

import static no.unit.nva.model.associatedartifacts.file.FileStatus.CANNOT_APPROVE_FILE_WITHOUT_LICENSE;

import java.util.UUID;

public class FileWithoutLicenseException extends IllegalStateException {

  public FileWithoutLicenseException(UUID fileIdentifier) {
    super(CANNOT_APPROVE_FILE_WITHOUT_LICENSE.formatted(fileIdentifier));
  }
}
