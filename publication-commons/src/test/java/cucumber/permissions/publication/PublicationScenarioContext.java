package cucumber.permissions.publication;

import static cucumber.permissions.PermissionsRole.ANY_CURATOR_TYPE;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCES_STANDARD;
import static nva.commons.apigateway.AccessRight.MANAGE_RESOURCE_FILES;
import cucumber.permissions.PermissionsRole;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.AccessRight;

public class PublicationScenarioContext {

    private PublicationOperation operation;
    private PublicationStatus publicationStatus = PublicationStatus.PUBLISHED;
    private Set<PermissionsRole> roles = new HashSet<>();

    public void setOperation(PublicationOperation operation) {
        this.operation = operation;
    }

    public void setRoles(Set<PermissionsRole> roles) {
        this.roles = roles;
    }

    public void setPublicationStatus(PublicationStatus status) {
        this.publicationStatus = status;
    }

    public PublicationPermissions getPublicationPermissions() {
        var topLevelOrgCristinId = randomUri();

        var access = getAccessRights(roles);

        var user = UserInstance.create(randomString(), randomUri(), randomUri(),
                                       access.stream().toList(),
                                       topLevelOrgCristinId);

        var currentUserIsContributor = roles.contains(PermissionsRole.OTHER_CONTRIBUTORS);
        var contributors = getContributors(user, currentUserIsContributor);

        var currentUserIsFileCurator = roles.contains(FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS);
        var curatingInstitutions = getCuratingInstitutions(currentUserIsFileCurator, user);

        var currentUserIsPublicationOwner = roles.contains(PermissionsRole.PUBLICATION_OWNER);
        var owner = getOwner(user, topLevelOrgCristinId, currentUserIsPublicationOwner);

        var randomResource = Resource.fromPublication(randomNonDegreePublication());

        var resource =
            randomResource.copy()
                .withStatus(publicationStatus)
                .withResourceOwner(owner)
                .withCuratingInstitutions(curatingInstitutions)
                .withEntityDescription(
                    randomResource.getEntityDescription().copy().withContributors(contributors).build())
                .build();

        return new PublicationPermissions(resource, user);
    }

    private static HashSet<CuratingInstitution> getCuratingInstitutions(boolean currentUserIsFileCurator,
                                                                        UserInstance user) {
        var curatingInstitutions = new HashSet<CuratingInstitution>();
        if (currentUserIsFileCurator) {
            curatingInstitutions.add(new CuratingInstitution(user.getTopLevelOrgCristinId(), Set.of()));
        }
        return curatingInstitutions;
    }

    private static Owner getOwner(UserInstance user, URI topLevelOrgCristinId, boolean currentUserIsOwner) {
        return currentUserIsOwner ? new Owner(user.getUser(), topLevelOrgCristinId)
                   : new Owner(new User(randomString()), topLevelOrgCristinId);
    }

    private static ArrayList<Contributor> getContributors(UserInstance user,
                                                          boolean addCurrentUserAsContributor) {
        var contributors = new ArrayList<Contributor>();
        if (addCurrentUserAsContributor) {
            contributors.add(new Contributor.Builder()
                                 .withAffiliations(List.of(Organization.fromUri(user.getTopLevelOrgCristinId())))
                                 .withIdentity(
                                     new Identity.Builder().withId(user.getPersonCristinId())
                                         .build())
                                 .withRole(new RoleType(Role.CREATOR)).build());
        }
        return contributors;
    }

    private static HashSet<AccessRight> getAccessRights(Set<PermissionsRole> roles) {
        var access = new HashSet<AccessRight>();
        if (roles.contains(FILE_CURATOR_BY_CONTRIBUTOR_FOR_OTHERS)) {
            access.add(MANAGE_RESOURCES_STANDARD);
            access.add(MANAGE_RESOURCE_FILES);
        }
        if (roles.contains(ANY_CURATOR_TYPE)) {
            access.add(MANAGE_RESOURCES_STANDARD);
        }
        return access;
    }

    public PublicationOperation getOperation() {
        return operation;
    }
}
