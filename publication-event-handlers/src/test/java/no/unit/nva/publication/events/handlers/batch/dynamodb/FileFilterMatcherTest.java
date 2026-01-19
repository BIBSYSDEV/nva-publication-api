package no.unit.nva.publication.events.handlers.batch.dynamodb;

import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.ImportSource;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.ImportUploadDetails;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.FileDao;
import no.unit.nva.publication.model.storage.ResourceDao;
import org.junit.jupiter.api.Test;

class FileFilterMatcherTest {

  private static final String SIKT_CRISTIN_ORG_ID = "20754.0.0.0";
  private static final URI SIKT_AFFILIATION =
      URI.create("https://api.nva.unit.no/cristin/organization/" + SIKT_CRISTIN_ORG_ID);
  private static final URI OTHER_AFFILIATION =
      URI.create("https://api.nva.unit.no/cristin/organization/194.0.0.0");

  private final FileFilterMatcher matcher = new FileFilterMatcher();

  @Test
  void shouldMatchFileWithScopusImportSourceFromEvent() {
    var fileEntry = createFileEntryWithImportSource(ImportSource.Source.SCOPUS, SIKT_AFFILIATION);
    var fileDao = FileDao.fromFileEntry(fileEntry);
    var filter = new BatchFilter(null, null, List.of("SCOPUS"), null);

    assertTrue(matcher.matches(fileDao, filter));
  }

  @Test
  void shouldMatchFileWithScopusImportSourceFromUploadDetails() {
    var file = createFileWithScopusUploadDetails();
    var fileEntry = createFileEntryWithFile(file, SIKT_AFFILIATION);
    var fileDao = FileDao.fromFileEntry(fileEntry);
    var filter = new BatchFilter(null, null, List.of("SCOPUS"), null);

    assertTrue(matcher.matches(fileDao, filter));
  }

  @Test
  void shouldNotMatchFileWithDifferentImportSource() {
    var fileEntry = createFileEntryWithImportSource(ImportSource.Source.BRAGE, SIKT_AFFILIATION);
    var fileDao = FileDao.fromFileEntry(fileEntry);
    var filter = new BatchFilter(null, null, List.of("SCOPUS"), null);

    assertFalse(matcher.matches(fileDao, filter));
  }

  @Test
  void shouldMatchFileWithSiktOwnerAffiliation() {
    var fileEntry = createFileEntryWithImportSource(ImportSource.Source.SCOPUS, SIKT_AFFILIATION);
    var fileDao = FileDao.fromFileEntry(fileEntry);
    var filter = new BatchFilter(null, null, null, List.of("20754."));

    assertTrue(matcher.matches(fileDao, filter));
  }

  @Test
  void shouldNotMatchFileWithDifferentOwnerAffiliation() {
    var fileEntry = createFileEntryWithImportSource(ImportSource.Source.SCOPUS, OTHER_AFFILIATION);
    var fileDao = FileDao.fromFileEntry(fileEntry);
    var filter = new BatchFilter(null, null, null, List.of("20754."));

    assertFalse(matcher.matches(fileDao, filter));
  }

  @Test
  void shouldMatchFileWhenBothFiltersApply() {
    var fileEntry = createFileEntryWithImportSource(ImportSource.Source.SCOPUS, SIKT_AFFILIATION);
    var fileDao = FileDao.fromFileEntry(fileEntry);
    var filter = new BatchFilter(null, null, List.of("SCOPUS"), List.of("20754."));

    assertTrue(matcher.matches(fileDao, filter));
  }

  @Test
  void shouldNotMatchFileWhenOnlyImportSourceMatches() {
    var fileEntry = createFileEntryWithImportSource(ImportSource.Source.SCOPUS, OTHER_AFFILIATION);
    var fileDao = FileDao.fromFileEntry(fileEntry);
    var filter = new BatchFilter(null, null, List.of("SCOPUS"), List.of("20754."));

    assertFalse(matcher.matches(fileDao, filter));
  }

  @Test
  void shouldReturnTrueWhenFilterIsEmpty() {
    var fileEntry = createFileEntryWithImportSource(ImportSource.Source.SCOPUS, SIKT_AFFILIATION);
    var fileDao = FileDao.fromFileEntry(fileEntry);
    var filter = new BatchFilter(null, null, null, null);

    assertTrue(matcher.matches(fileDao, filter));
  }

  @Test
  void shouldReturnFalseForNonFileDao() {
    var resourceDao = new ResourceDao();
    var filter = new BatchFilter(null, null, List.of("SCOPUS"), null);

    assertFalse(matcher.matches(resourceDao, filter));
  }

  private FileEntry createFileEntryWithImportSource(
      ImportSource.Source source, URI ownerAffiliation) {
    var userInstance = createUserInstance(ownerAffiliation);
    var file = randomOpenFile();
    var resourceIdentifier = SortableIdentifier.next();
    return FileEntry.createFromImportSource(
        file, resourceIdentifier, userInstance, ImportSource.fromSource(source));
  }

  private FileEntry createFileEntryWithFile(File file, URI ownerAffiliation) {
    var userInstance = createUserInstance(ownerAffiliation);
    var resourceIdentifier = SortableIdentifier.next();
    return FileEntry.create(file, resourceIdentifier, userInstance);
  }

  private UserInstance createUserInstance(URI ownerAffiliation) {
    return UserInstance.createBackendUser(
        new ResourceOwner(new Username(randomString()), ownerAffiliation), randomUri());
  }

  private File createFileWithScopusUploadDetails() {
    var uploadDetails =
        new ImportUploadDetails(ImportUploadDetails.Source.SCOPUS, null, Instant.now());
    return File.builder()
        .withIdentifier(java.util.UUID.randomUUID())
        .withName(randomString())
        .withMimeType("application/pdf")
        .withSize(1024L)
        .withLicense(randomUri())
        .withPublisherVersion(PublisherVersion.PUBLISHED_VERSION)
        .withRightsRetentionStrategy(
            NullRightsRetentionStrategy.create(
                RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY))
        .withUploadDetails(uploadDetails)
        .buildOpenFile();
  }
}
