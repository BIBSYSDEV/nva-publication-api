package cucumber.permissions.file;

import static cucumber.permissions.PermissionsRole.EXTERNAL_CLIENT;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_DEGREE;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_DEGREE_EMBARGO;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_FOR_GIVEN_FILE;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_FOR_OTHERS;
import static cucumber.permissions.PermissionsRole.FILE_OWNER;
import static cucumber.permissions.PermissionsRole.OTHER_CONTRIBUTORS;
import static cucumber.permissions.PermissionsRole.UNAUTHENTICATED;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import cucumber.permissions.PermissionsRole;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.Identity;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissions;
import nva.commons.apigateway.AccessRight;

public final class FileScenarioContext {

    private FileOperation fileOperation;
    private Class<File> fileType;
    private boolean fileBelongsToSameOrg = false;
    private boolean isDegree = false;
    private PublicationStatus publicationStatus = PublicationStatus.PUBLISHED;
    private Set<PermissionsRole> roles = new HashSet<>();
    private boolean isEmbargo = false;

    public void setFileType(String fileType) throws ClassNotFoundException {
        this.fileType = getFileType(fileType);
    }

    public void setIsDegree(boolean degree) {
        this.isDegree = degree;
    }

    public void setPublicationStatus(PublicationStatus publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    public void setRoles(Set<PermissionsRole> roles) {
        this.roles = roles;
    }

    public void setIsEmbargo(boolean embargo) {
        this.isEmbargo = embargo;
    }

    public void setFileBelongsToSameOrg(boolean fileBelongsToSameOrg) {
        this.fileBelongsToSameOrg = fileBelongsToSameOrg;
    }

    public FilePermissions getFilePermissions() {
        var access = getAccessRights(roles);

        var isUnauthenticated = roles.contains(UNAUTHENTICATED)|| roles.isEmpty();
        var isExternalClient = roles.contains(EXTERNAL_CLIENT);
        var user = getUserInstance(access, isUnauthenticated, isExternalClient);

        var topLevelOrgCristinId = getTopLevelOrgCristinId(user, roles);

        var currentUserIsContributor = roles.contains(OTHER_CONTRIBUTORS);
        var contributors =  getContributors(topLevelOrgCristinId, user, currentUserIsContributor);

        var randomResource = Resource.fromPublication(isDegree ? randomDegreePublication() : randomNonDegreePublication());

        var currentUserIsFileOwner = roles.contains(FILE_OWNER);
        var fileEntry = getFileEntry(topLevelOrgCristinId, user, randomResource, fileBelongsToSameOrg, isEmbargo,
                                     fileType, currentUserIsFileOwner);

        var customerId =  nonNull(user) ? user.getCustomerId() : randomUri();
        var resource = randomResource.copy()
                           .withStatus(publicationStatus)
                           .withPublisher(new Organization.Builder().withId(customerId).build())
                           .withEntityDescription(
                               randomResource.getEntityDescription().copy().withContributors(contributors).build())
                           .withCuratingInstitutions(
                               Set.of(new CuratingInstitution(topLevelOrgCristinId, Collections.emptySet())))
                           .build();

        return new FilePermissions(fileEntry, user, resource);
    }

    private static URI getTopLevelOrgCristinId(UserInstance user, Set<PermissionsRole> roles) {
        return nonNull(user) && isCurrentUserCuratorOnResource(roles) ? user.getTopLevelOrgCristinId() :  randomUri();
    }

    private static FileEntry getFileEntry(URI topLevelOrgCristinId, UserInstance user, Resource random,
                                          boolean fileBelongsToSameOrg, boolean isEmbargo,
                                          Class<File> fileType, boolean isOwner) {
        var fileAffiliation = fileBelongsToSameOrg ? topLevelOrgCristinId : randomUri();
        var fileOwner =  isOwner ? user : createInternalUser(Collections.emptySet(), fileAffiliation);
        return FileEntry.create(createFile(isEmbargo, fileType), random.getIdentifier(), fileOwner);
    }

    private static UserInstance getUserInstance(HashSet<AccessRight> access, boolean isUnauthenticated,
                                                boolean isExternalClient) {
        UserInstance user;
        if (isUnauthenticated) {
            user = null;
        } else {
            user = isExternalClient ? createExternalUser() : createInternalUser(access, randomUri());
        }
        return user;
    }

    private static HashSet<AccessRight> getAccessRights(Set<PermissionsRole> roles) {
        var access = new HashSet<AccessRight>();
        if (roles.contains(FILE_CURATOR_FOR_OTHERS)) {
            access.add(AccessRight.MANAGE_RESOURCES_STANDARD);
            access.add(AccessRight.MANAGE_RESOURCE_FILES);
        }

        if (roles.contains(FILE_CURATOR_DEGREE_EMBARGO)) {
            access.add(AccessRight.MANAGE_DEGREE);
            access.add(AccessRight.MANAGE_DEGREE_EMBARGO);
        }

        if (roles.contains(FILE_CURATOR_DEGREE)) {
            access.add(AccessRight.MANAGE_DEGREE);
        }

        if (roles.contains(FILE_CURATOR_FOR_GIVEN_FILE)) {
            access.add(AccessRight.MANAGE_RESOURCES_STANDARD);
            access.add(AccessRight.MANAGE_RESOURCE_FILES);
        }
        return access;
    }

    private static boolean isCurrentUserCuratorOnResource(Set<PermissionsRole> roles) {
        return Set.of(FILE_CURATOR_DEGREE, FILE_CURATOR_DEGREE_EMBARGO, FILE_CURATOR_FOR_GIVEN_FILE,
                      FILE_CURATOR_FOR_OTHERS)
                   .stream()
                   .anyMatch(roles::contains);
    }

    @SuppressWarnings("unchecked")
    private static Class<File> getFileType(String fileType) throws ClassNotFoundException {
        return (Class<File>) Class.forName(File.class.getPackageName() + "." + fileType);
    }

    private static UserInstance createExternalUser() {
        return UserInstance.createExternalUser(
            new ResourceOwner(new Username(randomString()), randomUri()), randomUri());
    }

    private static UserInstance createInternalUser(Set<AccessRight> accessRights, URI topLevelOrgCristinId) {
        return UserInstance.create(randomString(), randomUri(), randomUri(),
                                   accessRights.stream().toList(),
                                   topLevelOrgCristinId);
    }

    public void setFileOperation(FileOperation action) {
        this.fileOperation = action;
    }

    public FileOperation getFileOperation() {
        return fileOperation;
    }

    private static List<Contributor> getContributors(URI topLevelOrgCristinId, UserInstance user,
                                                     boolean isContributor) {
        return nonNull(user) && isContributor ? List.of(new Contributor.Builder().withAffiliations(
                List.of(Organization.fromUri(topLevelOrgCristinId)))
                                                            .withIdentity(
                                                                new Identity.Builder().withId(
                                                                        user.getPersonCristinId())
                                                                    .build())
                                                            .withRole(new RoleType(Role.CREATOR))
                                                            .build()) :
                                                                          new ArrayList<>();
    }

    private static File createFile(boolean isEmbargo, Class<File> fileType) {
        var file = File.builder();
        if (isEmbargo) {
            file.withEmbargoDate(Instant.now().plus(100, DAYS));
        }
        return file.build(fileType);
    }
}
