package no.unit.nva.publication.events.handlers.batch;

import static java.util.Objects.nonNull;

import java.net.URI;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

final class FileLicenseUpdater {

  private static final String LICENSE_PATH_FORMAT = "/associatedArtifacts/%s/license";
  private final ResourceService resourceService;

  private FileLicenseUpdater(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  static FileLicenseUpdater create(ResourceService resourceService) {
    return new FileLicenseUpdater(resourceService);
  }

  ResourceChange updateLicense(Resource resource, ManuallyUpdatePublicationsRequest request) {
    var filesToUpdate =
        resource.getFileEntries().stream()
            .filter(file -> hasLicense(request.oldValue(), file))
            .toList();
    var fieldChanges =
        filesToUpdate.stream().map(file -> licenseChange(file, request.newValue())).toList();

    if (!request.dryRun()) {
      filesToUpdate.forEach(file -> updateFileLicense(file, resource, request.newValue()));
    }
    return new ResourceChange(resource.getIdentifier().toString(), fieldChanges);
  }

  private static boolean hasLicense(String license, FileEntry fileEntry) {
    var fileLicense = fileEntry.getFile().getLicense();
    return nonNull(fileLicense) && fileLicense.toString().equals(license);
  }

  private FieldChange licenseChange(FileEntry fileEntry, String newLicense) {
    return new FieldChange(
        LICENSE_PATH_FORMAT.formatted(fileEntry.getIdentifier()),
        fileEntry.getFile().getLicense().toString(),
        newLicense);
  }

  private void updateFileLicense(FileEntry fileEntry, Resource resource, String license) {
    var updatedFile =
        fileEntry
            .getFile()
            .copy()
            .withLicense(URI.create(license))
            .build(fileEntry.getFile().getClass());
    fileEntry.update(
        updatedFile, UserInstance.fromPublication(resource.toPublication()), resourceService);
  }
}
