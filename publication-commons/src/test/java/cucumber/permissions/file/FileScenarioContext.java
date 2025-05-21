package cucumber.permissions.file;

import static cucumber.permissions.PermissionsRole.EXTERNAL_CLIENT;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_DEGREE;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_DEGREE_EMBARGO;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_FOR_GIVEN_FILE;
import static cucumber.permissions.PermissionsRole.OTHER_CONTRIBUTORS;
import static cucumber.permissions.PermissionsRole.PUBLICATION_OWNER;
import static cucumber.permissions.PermissionsRole.UNAUTHENTICATED;
import static cucumber.permissions.RolesToAccessRights.roleToAccessRightsMap;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.EVERYONE;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy.OWNER_ONLY;
import static no.unit.nva.publication.model.business.publicationchannel.ChannelType.PUBLISHER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import cucumber.permissions.FileOwner;
import cucumber.permissions.PermissionsRole;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.business.publicationchannel.ClaimedPublicationChannel;
import no.unit.nva.publication.model.business.publicationchannel.Constraint;
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
    private FileOwner fileOwnerType = FileOwner.OTHER_CONTRIBUTOR;
    private boolean hasClaimedPublisher;
    private boolean userBelongsToOrganizationThatClaimedPublisher;

    public Class<File> getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) throws ClassNotFoundException {
        this.fileType = getFileType(fileType);
    }

    public Set<PermissionsRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<PermissionsRole> roles) {
        this.roles = roles;
    }

    public void setIsDegree(boolean degree) {
        this.isDegree = degree;
    }

    public void setPublicationStatus(PublicationStatus publicationStatus) {
        this.publicationStatus = publicationStatus;
    }

    public void setIsEmbargo(boolean embargo) {
        this.isEmbargo = embargo;
    }

    public void setFileBelongsToSameOrg(boolean fileBelongsToSameOrg) {
        this.fileBelongsToSameOrg = fileBelongsToSameOrg;
    }

    public FilePermissions getFilePermissions() {
        var access = getAccessRights(roles);

        var isUnauthenticated = roles.contains(UNAUTHENTICATED) || roles.isEmpty();
        var isExternalClient = roles.contains(EXTERNAL_CLIENT);
        var user = getUserInstance(access, isUnauthenticated, isExternalClient);
        var curatorTopLevelOrgCristinId = getCuratorTopLevelOrgCristinId(user, roles);
        var publicationTopLevel = fileBelongsToSameOrg && nonNull(user)
                                      ? user.getTopLevelOrgCristinId()
                                      : randomUri();

        var publicationOwner = roles.contains(PUBLICATION_OWNER)
                                   ? user
                                   : createInternalUser(Collections.emptySet(), publicationTopLevel);

        var contributor = roles.contains(OTHER_CONTRIBUTORS)
                              ? user
                              : createInternalUser(Collections.emptySet(), curatorTopLevelOrgCristinId);

        var contributors = getContributors(contributor);

        var randomResource = Resource.fromPublication(
            isDegree ? randomDegreePublication() : randomNonDegreePublication());

        var fileOwner = getFileOwner(fileOwnerType, publicationOwner, contributor);

        var fileEntry = getFileEntry(fileOwner, randomResource, isEmbargo, fileType);

        var customerId = nonNull(user)
                             ? user.getCustomerId()
                             : publicationOwner.getCustomerId();

        var topLevelCristinId = nonNull(user)
                                    ? user.getTopLevelOrgCristinId()
                                    : publicationOwner.getTopLevelOrgCristinId();

        var claimedPublicationChannel = userBelongsToOrganizationThatClaimedPublisher
                                            ? createClaimedPublicationChannelWithinScope(randomResource, customerId,
                                                                                         topLevelCristinId)
                                            : createClaimedPublicationChannelWithinScope(randomResource, randomUri(),
                                                                                         randomUri());

        var resourceOwner = new Owner(publicationOwner.getUser(), publicationOwner.getTopLevelOrgCristinId());
        var entityDescription = randomResource.getEntityDescription().copy().withContributors(contributors).build();
        var curatingInstitutions = Set.of(new CuratingInstitution(curatorTopLevelOrgCristinId, Collections.emptySet()));

        var resource = randomResource.copy()
                           .withResourceOwner(resourceOwner)
                           .withStatus(publicationStatus)
                           .withPublisher(new Organization.Builder().withId(customerId).build())
                           .withEntityDescription(entityDescription)
                           .withCuratingInstitutions(curatingInstitutions)
                           .withPublicationChannels(hasClaimedPublisher
                                                        ? List.of(claimedPublicationChannel)
                                                        : List.of())
                           .build();

        return new FilePermissions(fileEntry, user, resource);
    }

    public void setHasClaimedPublisher(boolean hasClaimedPublisher) {
        this.hasClaimedPublisher = hasClaimedPublisher;
    }

    public void setUserBelongsToOrganizationThatClaimedPublisher(boolean userBelongs) {
        this.userBelongsToOrganizationThatClaimedPublisher = userBelongs;
    }

    public void setFileOwner(FileOwner fileOwner) {
        this.fileOwnerType = fileOwner;
    }

    public FileOperation getFileOperation() {
        return fileOperation;
    }

    public void setFileOperation(FileOperation action) {
        this.fileOperation = action;
    }

    private static UserInstance getFileOwner(FileOwner fileOwnerType, UserInstance publicationOwner,
                                             UserInstance contributor) {

        return switch (fileOwnerType) {
            case FileOwner.PUBLICATION_OWNER:
                yield publicationOwner;
            case FileOwner.CONTRIBUTOR_AT_X:
                yield contributor;
            case FileOwner.OTHER_CONTRIBUTOR:
                yield createInternalUser(Collections.emptySet(), randomUri());
        };
    }

    private static URI getCuratorTopLevelOrgCristinId(UserInstance user, Set<PermissionsRole> roles) {
        return nonNull(user) && isCurrentUserCuratorOnResource(roles) ? user.getTopLevelOrgCristinId() : randomUri();
    }

    private static FileEntry getFileEntry(UserInstance fileOwner, Resource random, boolean isEmbargo,
                                          Class<File> fileType) {
        return FileEntry.create(createFile(isEmbargo, fileType), random.getIdentifier(), fileOwner);
    }

    private static UserInstance getUserInstance(Set<AccessRight> access, boolean isUnauthenticated,
                                                boolean isExternalClient) {
        if (isUnauthenticated) {
            return null;
        }
        return isExternalClient ? createExternalUser() : createInternalUser(access, randomUri());
    }

    private static Set<AccessRight> getAccessRights(Set<PermissionsRole> roles) {
        return roles.stream()
                   .filter(roleToAccessRightsMap::containsKey)
                   .flatMap(role -> roleToAccessRightsMap.get(role).stream())
                   .collect(Collectors.toSet());
    }

    private static boolean isCurrentUserCuratorOnResource(Set<PermissionsRole> roles) {
        return Set.of(FILE_CURATOR_DEGREE, FILE_CURATOR_DEGREE_EMBARGO, FILE_CURATOR_FOR_GIVEN_FILE,
                      FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS)
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

    private static List<Contributor> getContributors(UserInstance user) {
        return List.of(createContributor(user));
    }

    private static Contributor createContributor(UserInstance user) {
        return new Contributor.Builder().withAffiliations(
                List.of(Organization.fromUri(user.getTopLevelOrgCristinId())))
                   .withIdentity(
                       new Identity.Builder().withId(
                               user.getPersonCristinId())
                           .build())
                   .withRole(new RoleType(Role.CREATOR))
                   .build();
    }

    private static File createFile(boolean isEmbargo, Class<File> fileType) {
        var file = File.builder();
        if (isEmbargo) {
            file.withEmbargoDate(Instant.now().plus(100, DAYS));
        }
        return file.build(fileType);
    }

    private ClaimedPublicationChannel createClaimedPublicationChannelWithinScope(Resource resource, URI customerId,
                                                                                 URI organizationId) {
        return new ClaimedPublicationChannel(randomUri(),
                                             customerId,
                                             organizationId,
                                             new Constraint(
                                                 EVERYONE,
                                                 OWNER_ONLY,
                                                 List.of(resource.getInstanceType().orElseThrow())),
                                             PUBLISHER,
                                             SortableIdentifier.next(),
                                             resource.getIdentifier(),
                                             Instant.now(),
                                             Instant.now());
    }
}
