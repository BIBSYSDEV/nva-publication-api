package no.unit.nva.publication.permissions.file;

import static java.util.UUID.randomUUID;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomOpenFile;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE;
import static nva.commons.apigateway.AccessRight.MANAGE_DEGREE_EMBARGO;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.util.List;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.instancetypes.degree.DegreeBachelor;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ChannelType;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import nva.commons.apigateway.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

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

    @ParameterizedTest
    @EnumSource(value = FileOperation.class, mode = Mode.INCLUDE, names = {"WRITE_METADATA", "DELETE"})
    void shouldAllowFileCuratorOnFileForResourceWithClaimedPublisherOwnedByCuratorOrganization(FileOperation fileOperation) {
        var institutionId = getInstitutionId();
        var claimedPublisher = createClaimedPublisher(institutionId);
        var resource = randomResource();
        resource.setPublicationChannels(List.of(claimedPublisher));
        var userInstance = fileCuratorUserInstance(institutionId);
        var fileEntry = FileEntry.create(randomOpenFile(), resource.getIdentifier(), userInstance);

        assertTrue(FilePermissions.create(fileEntry, userInstance, resource).allowsAction(fileOperation));
    }

    @ParameterizedTest
    @EnumSource(value = FileOperation.class, mode = Mode.INCLUDE, names = {"WRITE_METADATA", "DELETE"})
    void shouldAllowExternalClientOnFileForResourceWithClaimedPublisherWhenClientRelatesToPublication(FileOperation fileOperation) {
        var institutionId = getInstitutionId();
        var claimedPublisher = createClaimedPublisher(institutionId);
        var publication = randomPublication(DegreeBachelor.class);
        var resource = Resource.fromPublication(publication);
        resource.setPublicationChannels(List.of(claimedPublisher));
        var userInstance = UserInstance.createExternalUser(publication.getResourceOwner(), publication.getPublisher().getId());
        var fileEntry = FileEntry.create(randomOpenFile(), resource.getIdentifier(), userInstance);

        assertTrue(FilePermissions.create(fileEntry, userInstance, resource).allowsAction(fileOperation));
    }

    @ParameterizedTest
    @EnumSource(value = FileOperation.class, mode = Mode.INCLUDE, names = {"WRITE_METADATA", "DELETE"})
    void shouldDenyFileCuratorOnFileForResourceWithClaimedPublisherNotOwnedByCuratorInstitution(FileOperation fileOperation) {
        var claimedPublisher = createClaimedPublisher(randomUri());
        var resource = randomResource();
        resource.setPublicationChannels(List.of(claimedPublisher));
        var userInstance = fileCuratorUserInstance(randomUri());
        var fileEntry = FileEntry.create(randomOpenFile(), resource.getIdentifier(), userInstance);

        assertFalse(FilePermissions.create(fileEntry, userInstance, resource).allowsAction(fileOperation));
    }

    private static UserInstance fileCuratorUserInstance(URI institutionId) {
        return UserInstance.create(randomString(), randomUri(), randomUri(),
                                   List.of(MANAGE_RESOURCE_FILES, MANAGE_DEGREE, MANAGE_DEGREE_EMBARGO),
                                   institutionId);
    }

    private static URI getInstitutionId() {
        return randomUri();
    }

    private ClaimedPublicationChannel createClaimedPublisher(URI institutionId) {
        var channel = mock(ClaimedPublicationChannel.class);
        when(channel.getOrganizationId()).thenReturn(institutionId);
        when(channel.getChannelType()).thenReturn(ChannelType.PUBLISHER);
        return channel;
    }

    private static Resource randomResource() {
        return Resource.fromPublication(randomPublication());
    }

    private static FilePermissions getFilePermissions(File file) {
        var fileEntry = FileEntry.create(file, SortableIdentifier.next(),
                                         UserInstance.create(randomString(), randomUri()));
        var resource = Resource.fromPublication(randomNonDegreePublication()).copy().withStatus(PUBLISHED).build();
        return FilePermissions.create(fileEntry,
                                      UserInstance.create(randomString(), randomUri()),
                                      resource);
    }
}
