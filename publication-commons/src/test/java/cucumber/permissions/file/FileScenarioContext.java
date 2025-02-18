package cucumber.permissions.file;

import static cucumber.permissions.file.FileScenarioContext.FileRelationship.SAME_ORG;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.Organization;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.Identity;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public class FileScenarioContext {

    private final FileContext fileContext = new FileContext();
    private final UserScenarioContext userContext = new UserScenarioContext();
    private FileOperation fileOperation;
    private Resource resource;

    public void setFile(File file) {
        fileContext.file = file;
    }

    public void setFileRelationship(FileRelationship owner) {
        fileContext.relationship = owner;
    }

    public FileEntry getFileEntry() {
        var owner = fileContext.relationship == FileRelationship.OWNER ? getCurrentUserInstance() : getOtherUserInstance();
        return FileEntry.create(fileContext.file, getResource().getIdentifier(), owner);
    }

    public UserInstance getCurrentUserInstance() {
        return isInternalUser() ? createInternalUser() : createExternalUser();
    }

    public void setCurrentUserAsDegreeEmbargoFileCuratorForGivenFile() {
        addUserRole(AccessRight.MANAGE_DEGREE);
        addUserRole(AccessRight.MANAGE_DEGREE_EMBARGO);

        var topLevelOrgCristinId = getTopLevelOrgCristinId();
        var curatingInstitutions = Set.of(new CuratingInstitution(topLevelOrgCristinId, Collections.emptySet()));

        getResource().setCuratingInstitutions(curatingInstitutions);
    }

    public void setCurrentUserAsFileCuratorForGivenFile() {
        setCurrentUserAsFileCurator();
        setFileRelationship(SAME_ORG);
    }

    public void setCurrentUserAsDegreeFileCuratorForGivenFile() {
        addUserRole(AccessRight.MANAGE_DEGREE);
        setFileRelationship(SAME_ORG);

        var topLevelOrgCristinId = getTopLevelOrgCristinId();
        var curatingInstitutions = Set.of(new CuratingInstitution(topLevelOrgCristinId, Collections.emptySet()));

        getResource().setCuratingInstitutions(curatingInstitutions);
    }

    private UserInstance createExternalUser() {
        return UserInstance.createExternalUser(
            new ResourceOwner(new Username(userContext.userIdentifier), userContext.topLevelOrgCristinId),
            userContext.customerId);
    }

    private UserInstance createInternalUser() {
        return UserInstance.create(userContext.userIdentifier, userContext.customerId, userContext.personCristinId,
                                   userContext.accessRights.stream().toList(),
                                   userContext.topLevelOrgCristinId);
    }

    private boolean isInternalUser() {
        return userContext.userClientType == UserClientType.INTERNAL;
    }

    public UserInstance getOtherUserInstance() {
        var otherUserContext = new UserScenarioContext();
        return UserInstance.create(otherUserContext.userIdentifier, otherUserContext.customerId,
                                   otherUserContext.personCristinId,
                                   otherUserContext.accessRights.stream().toList(),
                                   fileContext.relationship.equals(SAME_ORG) ?
                                       userContext.topLevelOrgCristinId : randomUri());
    }

    public void setFileOperation(FileOperation action) {
        this.fileOperation = action;
    }

    public FileOperation getFileOperation() {
        return fileOperation;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public void addUserRole(AccessRight accessRight) {
        userContext.accessRights.add(accessRight);
    }

    public URI getTopLevelOrgCristinId() {
        return userContext.topLevelOrgCristinId;
    }

    public void setUserClientType(UserClientType userClientType) {
        userContext.userClientType = userClientType;
    }

    public void setPublisherId(URI customerId) {
        resource.setPublisher(new Organization.Builder().withId(customerId).build());
    }

    public void addCurrentUserAndTopLevelAsContributor() {
        var contributor =
            new Contributor.Builder().withAffiliations(
                    List.of(Organization.fromUri(getTopLevelOrgCristinId())))
                .withIdentity(
                    new Identity.Builder().withId(getCurrentUserInstance().getPersonCristinId())
                        .build())
                .withRole(new RoleType(Role.CREATOR)).build();
        getResource().getEntityDescription().setContributors(List.of(contributor));
    }

    public void setCurrentUserAsFileCurator() {
        addUserRole(AccessRight.MANAGE_RESOURCES_STANDARD);
        addUserRole(AccessRight.MANAGE_RESOURCE_FILES);

        var topLevelOrgCristinId = getTopLevelOrgCristinId();
        var curatingInstitutions = Set.of(new CuratingInstitution(topLevelOrgCristinId, Collections.emptySet()));

        getResource().setCuratingInstitutions(curatingInstitutions);
    }

    public static class UserScenarioContext {

        public String userIdentifier = randomString();
        public URI customerId = randomUri();
        public URI personCristinId = randomUri();
        public Set<AccessRight> accessRights = new HashSet<>();
        public URI topLevelOrgCristinId = randomUri();
        public UserClientType userClientType = UserClientType.INTERNAL;
    }

    public static class FileContext {

        public File file;
        public FileRelationship relationship = FileRelationship.NO_RELATION;
    }

    public enum FileRelationship {
        OWNER,
        NO_RELATION,
        SAME_ORG
    }
}
