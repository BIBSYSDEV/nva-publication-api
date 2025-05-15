package no.unit.nva.publication.model.business;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomHiddenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingInternalFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.publicationstate.FileTypeUpdatedEvent;
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

    private static UserInstance randomUserInstance() {
        return new UserInstance(randomHiddenFile().toJsonString(), randomUri(), randomUri(), randomUri(),
                                randomUri(), List.of(),
                                UserClientType.INTERNAL);
    }
}
