package no.unit.nva.publication.model.business;

import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.NULL_RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration.RIGHTS_RETENTION_STRATEGY;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomHiddenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.NullRightsRetentionStrategy;
import no.unit.nva.model.associatedartifacts.RightsRetentionStrategyConfiguration;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PublisherVersion;
import no.unit.nva.model.associatedartifacts.file.UserUploadDetails;
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

        assertEquals(new SortableIdentifier(file.getIdentifier().toString()), fileEntry.getIdentifier());
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

        assertThat(Objects.requireNonNull(fileEntry.getFile().getPublishedDate().orElse(null)),
                   is(equalTo(originalPublishedDate)));
    }

    private static @NonNull OpenFile getFile(UUID fileIdentifier, RightsRetentionStrategyConfiguration rrs,
                                             Instant originalPublishedDate) {
        var license = URI.create("https://creativecommons.org/licenses/by/4.0/");
        var uploadDetails = new UserUploadDetails(new Username(randomString()), Instant.now());

        return new OpenFile(fileIdentifier, "test.pdf", "application/pdf", 1024L, license,
                            PublisherVersion.PUBLISHED_VERSION, null, NullRightsRetentionStrategy.create(rrs), null,
                            originalPublishedDate, uploadDetails);
    }

    private static UserInstance randomUserInstance() {
        return new UserInstance(randomHiddenFile().toJsonString(), randomUri(), randomUri(), randomUri(),
                                randomUri(), List.of(),
                                UserClientType.INTERNAL, null);
    }
}
