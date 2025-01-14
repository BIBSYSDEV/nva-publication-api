package no.unit.nva.publication.permissions.file;

import static java.util.UUID.randomUUID;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;

class FilePermissionTest {

    @Test
    void shouldListReadForOpenFileWhenRandomUser() {
        var permissions = FilePermissions.create(File.builder().withIdentifier(randomUUID()).buildOpenFile(),
                                                 UserInstance.create(randomString(), randomUri()));

        var actual = permissions.getAllAllowedActions();

        assertThat(actual, hasItems(FileOperation.READ_METADATA));
    }

    @Test
    void shouldThrowOnAuthorizeOnReadForRandomUserWhenFileIsHidden() {
        var permissions = FilePermissions.create(File.builder().withIdentifier(randomUUID()).buildHiddenFile(),
                                                 UserInstance.create(randomString(), randomUri()));

        assertThrows(UnauthorizedException.class, () -> permissions.authorize(FileOperation.READ_METADATA));
    }

    @Test
    void shouldThrowOnAuthorizeOnWriteWhenRandomUserAndNoHitOnStrategies() {
        var permissions = FilePermissions.create(
            File.builder().withIdentifier(randomUUID()).buildPendingInternalFile(),
            UserInstance.create(randomString(), randomUri()));

        assertThrows(UnauthorizedException.class, () -> permissions.authorize(FileOperation.WRITE_METADATA));
    }

    @Test
    void shouldNotThrowOnAuthorizeWhenReadForOpenFileAndRandomUser() {
        var permissions = FilePermissions.create(File.builder().withIdentifier(randomUUID()).buildOpenFile(),
                                                 UserInstance.create(randomString(), randomUri()));

        assertDoesNotThrow(() -> permissions.authorize(FileOperation.READ_METADATA));
    }
}
