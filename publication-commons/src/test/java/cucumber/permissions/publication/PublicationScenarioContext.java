package cucumber.permissions.publication;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import nva.commons.apigateway.AccessRight;

public class PublicationScenarioContext {

    private final UserScenarioContext userContext = new UserScenarioContext();
    private Resource resource;
    private PublicationOperation operation;

    public UserInstance getCurrentUserInstance() {
        return UserInstance.create(userContext.userIdentifier, userContext.customerId, userContext.personCristinId,
                                   userContext.accessRights.stream().toList(),
                                   userContext.topLevelOrgCristinId);
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

    public void setOperation(PublicationOperation operation) {
        this.operation = operation;
    }

    public PublicationOperation getOperation() {
        return operation;
    }

    public static class UserScenarioContext {

        public String userIdentifier = randomString();
        public URI customerId = randomUri();
        public URI personCristinId = randomUri();
        public Set<AccessRight> accessRights = new HashSet<>();
        public URI topLevelOrgCristinId = randomUri();
    }
}
