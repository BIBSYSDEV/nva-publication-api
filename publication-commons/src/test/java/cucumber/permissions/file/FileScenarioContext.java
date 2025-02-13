package cucumber.permissions.file;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.Organization;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
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

    public void setFileOwnership(FileOwnership owner) {
        fileContext.ownership = owner;
    }

    public FileEntry getFileEntry() {
        var owner = fileContext.ownership == FileOwnership.OWNER ? getCurrentUserInstance() : getOtherUserInstance();
        return FileEntry.create(fileContext.file, getResource().getIdentifier(), owner);
    }

    public UserInstance getCurrentUserInstance() {
        if (userContext.userClientType == UserClientType.INTERNAL) {
            return UserInstance.create(userContext.userIdentifier, userContext.customerId, userContext.personCristinId,
                                       userContext.accessRights.stream().toList(),
                                       userContext.topLevelOrgCristinId);
        } else {
            return UserInstance.createExternalUser(
                new ResourceOwner(new Username(userContext.userIdentifier), userContext.topLevelOrgCristinId),
                userContext.customerId);
        }
    }

    public UserInstance getOtherUserInstance() {
        var otherUserContext = new UserScenarioContext();
        return UserInstance.create(otherUserContext.userIdentifier, otherUserContext.customerId,
                                   otherUserContext.personCristinId,
                                   otherUserContext.accessRights.stream().toList(),
                                   otherUserContext.topLevelOrgCristinId);
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
        public FileOwnership ownership;
    }

    public enum FileOwnership {
        OWNER,
        NOT_OWNER
    }
}
