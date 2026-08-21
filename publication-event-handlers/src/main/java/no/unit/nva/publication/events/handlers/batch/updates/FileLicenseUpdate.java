package no.unit.nva.publication.events.handlers.batch.updates;

import static java.util.Objects.nonNull;

import java.net.URI;
import no.unit.nva.publication.events.handlers.batch.ManualUpdate;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

public final class FileLicenseUpdate implements ManualUpdate {

  private final ResourceService resourceService;

  public FileLicenseUpdate(ResourceService resourceService) {
    this.resourceService = resourceService;
  }

  @Override
  public ManualUpdateType type() {
    return ManualUpdateType.LICENSE;
  }

  @Override
  public boolean matches(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return resource.getFileEntries().stream()
        .anyMatch(fileEntry -> hasLicense(request.oldValue(), fileEntry));
  }

  @Override
  public void apply(Resource resource, ManuallyUpdatePublicationsRequest request) {
    resource.getFileEntries().stream()
        .filter(fileEntry -> hasLicense(request.oldValue(), fileEntry))
        .forEach(fileEntry -> updateFileLicense(fileEntry, resource, request.newValue()));
  }

  private static boolean hasLicense(String license, FileEntry fileEntry) {
    var fileLicense = fileEntry.getFile().getLicense();
    return nonNull(fileLicense) && fileLicense.toString().equals(license);
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
