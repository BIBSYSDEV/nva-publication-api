package no.unit.nva.publication.model.business;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomHiddenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ImportSource.Source;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.OverriddenRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.FileStatus;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
import no.unit.nva.publication.model.business.publicationstate.FileTypeUpdatedByImportEvent;
import no.unit.nva.publication.model.business.publicationstate.FileTypeUpdatedEvent;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class FileEntryTest {

  @Test
  void shouldCreateFileEntryFromFileWithCorrectData() {
    var file = randomOpenFile();
    var userInstance = randomUserInstance();
    var resourceIdentifier = SortableIdentifier.next();
    var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);

    assertEquals(
        new SortableIdentifier(file.getIdentifier().toString()), fileEntry.getIdentifier());
    assertEquals(resourceIdentifier, fileEntry.getResourceIdentifier());
    assertEquals(userInstance.getTopLevelOrgCristinId(), fileEntry.getOwnerAffiliation());
    assertEquals(userInstance.getUser(), fileEntry.getOwner());
    assertEquals(userInstance.getCustomerId(), fileEntry.getCustomerId());
  }

  @Test
  void shouldReturnFile() {
    var file = randomOpenFile();
    var userInstance = randomUserInstance();
    var resourceIdentifier = SortableIdentifier.next();
    var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);

    assertEquals(file, fileEntry.getFile());
  }

  @Test
  void shouldReturnFalseWhenFileEventIsNull() {
    var file = randomOpenFile();
    var userInstance = randomUserInstance();
    var resourceIdentifier = SortableIdentifier.next();
    var fileEntry = FileEntry.create(file, resourceIdentifier, userInstance);

    assertFalse(fileEntry.hasFileEvent());
  }

  @Test
  void queryObjectShouldReturnQueryObject() {
    var file = randomOpenFile();
    var resourceIdentifier = SortableIdentifier.next();
    var queryObject = FileEntry.queryObject(file.getIdentifier(), resourceIdentifier);

    assertInstanceOf(QueryObject.class, queryObject);
  }

  @Test
  void shouldSetFileTypeEventToFileUpdatedWhenUpdatingFileTypeOfPendingFileToAnotherPendingFile() {
    var pendingInternalFile = randomPendingInternalFile();
    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(pendingInternalFile, SortableIdentifier.next(), userInstance);
    fileEntry.update(pendingInternalFile.toPendingOpenFile(), userInstance);

    assertInstanceOf(FileTypeUpdatedEvent.class, fileEntry.getFileEvent());
  }

  @Test
  void shouldPreservePublishedDateWhenOnlyRrsConfiguredTypeDiffers() {
    var originalPublishedDate = Instant.parse("2026-01-27T14:51:47.353704642Z");
    var fileIdentifier = UUID.randomUUID();

    var storedFile = getFile(fileIdentifier, NULL_RIGHTS_RETENTION_STRATEGY, originalPublishedDate);

    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(storedFile, SortableIdentifier.next(), userInstance);

    var incomingFile = getFile(fileIdentifier, RIGHTS_RETENTION_STRATEGY, originalPublishedDate);

    fileEntry.update(incomingFile, userInstance);

    assertThat(
        Objects.requireNonNull(fileEntry.getFile().getPublishedDate().orElse(null)),
        is(equalTo(originalPublishedDate)));
  }

  private static @NonNull OpenFile getFile(
      UUID fileIdentifier,
      RightsRetentionStrategyConfiguration rrs,
      Instant originalPublishedDate) {
    var license = URI.create("https://creativecommons.org/licenses/by/4.0/");
    var uploadDetails = new UserUploadDetails(new Username(randomString()), Instant.now());

    return new OpenFile(
        fileIdentifier,
        "test.pdf",
        "application/pdf",
        1024L,
        license,
        PublisherVersion.PUBLISHED_VERSION,
        null,
        NullRightsRetentionStrategy.create(rrs),
        null,
        originalPublishedDate,
        uploadDetails);
  }

  @Test
  void shouldNotUpdateFileWhenIncomingFileIsEqualToStoredFile() {
    var file = randomOpenFile();
    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(file, SortableIdentifier.next(), userInstance);
    var modifiedDateBefore = fileEntry.getModifiedDate();

    fileEntry.updateFromImport(file, userInstance, ImportSource.fromSource(Source.CRISTIN));

    assertThat(fileEntry.getFile(), is(equalTo(file)));
    assertThat(fileEntry.getModifiedDate(), is(equalTo(modifiedDateBefore)));
    assertNull(fileEntry.getFileEvent());
  }

  @Test
  void shouldUpdateFilePropertiesFromIncomingFileWhenFilesAreDifferent() {
    var storedFile = randomPendingOpenFile();
    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(storedFile, SortableIdentifier.next(), userInstance);

    var incomingFile =
        storedFile
            .copy()
            .withLicense(URI.create("https://creativecommons.org/licenses/by/4.0/"))
            .withPublisherVersion(PublisherVersion.PUBLISHED_VERSION)
            .withEmbargoDate(Instant.now().plus(90, ChronoUnit.DAYS))
            .withLegalNote(randomString())
            .withRightsRetentionStrategy(
                OverriddenRightsRetentionStrategy.create(
                    RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY, randomString()))
            .buildPendingOpenFile();

    fileEntry.updateFromImport(incomingFile, userInstance, ImportSource.fromSource(Source.CRISTIN));

    var updatedFile = fileEntry.getFile();
    assertThat(updatedFile.getLicense(), is(equalTo(incomingFile.getLicense())));
    assertThat(updatedFile.getPublisherVersion(), is(equalTo(incomingFile.getPublisherVersion())));
    assertThat(updatedFile.getEmbargoDate(), is(equalTo(incomingFile.getEmbargoDate())));
    assertThat(updatedFile.getLegalNote(), is(equalTo(incomingFile.getLegalNote())));
    assertThat(
        updatedFile.getRightsRetentionStrategy(),
        is(equalTo(incomingFile.getRightsRetentionStrategy())));
  }

  @Test
  void shouldPreserveFileIdentityFromStoredFileWhenUpdatingFromImport() {
    var storedFile = randomPendingOpenFile();
    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(storedFile, SortableIdentifier.next(), userInstance);

    var incomingFile = randomPendingOpenFile();

    fileEntry.updateFromImport(incomingFile, userInstance, ImportSource.fromSource(Source.CRISTIN));

    var updatedFile = fileEntry.getFile();
    assertThat(updatedFile.getIdentifier(), is(equalTo(storedFile.getIdentifier())));
    assertThat(updatedFile.getName(), is(equalTo(storedFile.getName())));
    assertThat(updatedFile.getMimeType(), is(equalTo(storedFile.getMimeType())));
    assertThat(updatedFile.getSize(), is(equalTo(storedFile.getSize())));
  }

  @Test
  void shouldSetFileTypeFromIncomingFileWhenUpdatingFromImport() {
    var storedFile = randomPendingOpenFile();
    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(storedFile, SortableIdentifier.next(), userInstance);

    var incomingFile = storedFile.toPendingInternalFile();

    fileEntry.updateFromImport(incomingFile, userInstance, ImportSource.fromSource(Source.CRISTIN));

    assertThat(FileStatus.from(fileEntry.getFile()), is(equalTo(FileStatus.PENDING_INTERNAL)));
  }

  @Test
  void shouldSetImportEventWhenFilesAreDifferent() {
    var storedFile = randomPendingOpenFile();
    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(storedFile, SortableIdentifier.next(), userInstance);

    var incomingFile = randomPendingOpenFile();
    var importSource = ImportSource.fromBrageArchive(randomString());

    fileEntry.updateFromImport(incomingFile, userInstance, importSource);

    assertInstanceOf(FileTypeUpdatedByImportEvent.class, fileEntry.getFileEvent());
  }

  @Test
  void shouldUpdateModifiedDateWhenFilesAreDifferent() {
    var storedFile = randomPendingOpenFile();
    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(storedFile, SortableIdentifier.next(), userInstance);
    var modifiedDateBefore = fileEntry.getModifiedDate();

    var incomingFile = randomPendingOpenFile();
    fileEntry.updateFromImport(incomingFile, userInstance, ImportSource.fromSource(Source.CRISTIN));

    assertThat(fileEntry.getModifiedDate(), is(notNullValue()));
    assertFalse(fileEntry.getModifiedDate().equals(modifiedDateBefore));
  }

  @Test
  void shouldReturnSameFileEntryInstance() {
    var storedFile = randomPendingOpenFile();
    var userInstance = UserInstance.create(randomString(), randomUri());
    var fileEntry = FileEntry.create(storedFile, SortableIdentifier.next(), userInstance);

    var result =
        fileEntry.updateFromImport(
            randomPendingOpenFile(), userInstance, ImportSource.fromSource(Source.CRISTIN));

    assertThat(result, is(sameInstance(fileEntry)));
  }

  private static UserInstance randomUserInstance() {
    return new UserInstance(
        randomHiddenFile().toJsonString(),
        randomUri(),
        randomUri(),
        randomUri(),
        randomUri(),
        List.of(),
        UserClientType.INTERNAL,
        null);
  }
}
