package no.unit.nva.publication.permissions.file;

import static java.util.UUID.randomUUID;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;

class FilePermissionTest {

    @Test
    void shouldReturnAllowedActionsSuccessfullyAsRandomUser() {
        var permissions = getFilePermissions(File.builder().withIdentifier(randomUUID()).buildOpenFile());

        var actual = permissions.getAllAllowedActions();

        assertThat(actual, hasItems(FileOperation.READ_METADATA));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenReadingHiddenFileAsRandomUser() {
        var permissions = getFilePermissions(File.builder().withIdentifier(randomUUID()).buildHiddenFile());

        assertThrows(UnauthorizedException.class, () -> permissions.authorize(FileOperation.READ_METADATA));
    }

    @Test
    void shouldThrowUnauthorizedExceptionWhenAuthorizingWriteAsRandomUserAndNoHitOnStrategies() {
        var permissions = getFilePermissions(File.builder().withIdentifier(randomUUID()).buildPendingInternalFile());

        assertThrows(UnauthorizedException.class, () -> permissions.authorize(FileOperation.WRITE_METADATA));
    }

    @Test
    void shouldNotThrowExceptionWhenAuthorizingReadForOpenFileAndRandomUser() {
        var permissions = getFilePermissions(File.builder().withIdentifier(randomUUID()).buildOpenFile());

        assertDoesNotThrow(() -> permissions.authorize(FileOperation.READ_METADATA));
    }

    private static FilePermissions getFilePermissions(File file) {
        var fileEntry = FileEntry.create(file, SortableIdentifier.next(), UserInstance.create(randomString(), randomUri()));
        return FilePermissions.create(fileEntry,
                                      UserInstance.create(randomString(), randomUri()),
                                      Resource.fromPublication(randomNonDegreePublication()));
    }
}
