package no.unit.nva.publication.events.handlers.batch.updates;

import static java.util.Objects.nonNull;

import java.net.URI;
import java.util.List;
import no.unit.nva.publication.events.handlers.batch.FieldChange;
import no.unit.nva.publication.events.handlers.batch.ManualUpdate;
import no.unit.nva.publication.events.handlers.batch.ManualUpdateType;
import no.unit.nva.publication.events.handlers.batch.ManuallyUpdatePublicationsRequest;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.ResourceService;

public final class FileLicenseUpdate implements ManualUpdate {

  private static final String FILE_LICENSE_PATH_FORMAT = "file:%s/license";
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
  public List<FieldChange> plan(Resource resource, ManuallyUpdatePublicationsRequest request) {
    return filesToUpdate(resource, request).stream()
        .map(fileEntry -> licenseChange(fileEntry, request))
        .toList();
  }

  @Override
  public void commit(Resource resource, ManuallyUpdatePublicationsRequest request) {
    filesToUpdate(resource, request)
        .forEach(fileEntry -> updateFileLicense(fileEntry, resource, request));
  }

  private static boolean hasLicense(String license, FileEntry fileEntry) {
    var fileLicense = fileEntry.getFile().getLicense();
    return nonNull(fileLicense) && fileLicense.toString().equals(license);
  }

  private List<FileEntry> filesToUpdate(
      Resource resource, ManuallyUpdatePublicationsRequest request) {
    if (request.oldValue().equals(request.newValue())) {
      return List.of();
    }
    return resource.getFileEntries().stream()
        .filter(fileEntry -> hasLicense(request.oldValue(), fileEntry))
        .toList();
  }

  private FieldChange licenseChange(
      FileEntry fileEntry, ManuallyUpdatePublicationsRequest request) {
    return new FieldChange(
        FILE_LICENSE_PATH_FORMAT.formatted(fileEntry.getIdentifier()),
        fileEntry.getFile().getLicense().toString(),
        request.newValue());
  }

  private void updateFileLicense(
      FileEntry fileEntry, Resource resource, ManuallyUpdatePublicationsRequest request) {
    var updatedFile =
        fileEntry
            .getFile()
            .copy()
            .withLicense(URI.create(request.newValue()))
            .build(fileEntry.getFile().getClass());
    fileEntry.update(
        updatedFile, UserInstance.fromPublication(resource.toPublication()), resourceService);
  }
}
