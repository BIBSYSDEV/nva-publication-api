package cucumber.permissions.publication;

import static cucumber.permissions.PermissionsRole.CONTRIBUTOR_FOR_GIVEN_FILE;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR;
import static cucumber.permissions.PermissionsRole.PUBLICATION_OWNER;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.PermissionsRole;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import nva.commons.apigateway.AccessRight;

public class PublicationAccessFeatures {

    private final PublicationScenarioContext scenarioContext;

    public PublicationAccessFeatures(PublicationScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a file of type {string}")
    public void aFileOfTheType(String string) {
        scenarioContext.setResource(Resource.fromPublication(randomNonDegreePublication()));
    }

    @When("the user have the role {string}")
    public void theUserHaveTheRole(String userRole) {
        var roles = PermissionsRole.lookup(userRole);
        if (roles.contains(FILE_CURATOR)) {
            scenarioContext.addUserRole(AccessRight.MANAGE_RESOURCES_STANDARD);
            scenarioContext.addUserRole(AccessRight.MANAGE_RESOURCE_FILES);

            var topLevelOrgCristinId = scenarioContext.getTopLevelOrgCristinId();
            var curatingInstitutions = Set.of(new CuratingInstitution(topLevelOrgCristinId, Collections.emptySet()));

            scenarioContext.getResource().setCuratingInstitutions(curatingInstitutions);
        }
        if (roles.contains(PUBLICATION_OWNER)) {
            scenarioContext.getResource().setResourceOwner(new Owner(scenarioContext.getCurrentUserInstance().getUser(), scenarioContext.getTopLevelOrgCristinId()));
        }
        if (roles.contains(CONTRIBUTOR_FOR_GIVEN_FILE) || roles.contains(PermissionsRole.OTHER_CONTRIBUTORS)) {
            var contributor =
                new Contributor.Builder().withAffiliations(
                        List.of(Organization.fromUri(scenarioContext.getTopLevelOrgCristinId())))
                    .withIdentity(
                        new Identity.Builder().withId(scenarioContext.getCurrentUserInstance().getPersonCristinId())
                            .build())
                    .withRole(new RoleType(Role.CREATOR)).build();
            scenarioContext.getResource().getEntityDescription().setContributors(List.of(contributor));

        }
    }

    @And("the user attempts to {string}")
    public void theUserAttemptsTo(String operation) {
        scenarioContext.setOperation(PublicationOperation.lookup(operation));
    }

    @Then("the action outcome is {string}")
    public void theActionOutcomeIs(String outcome) {
        var permissions = new PublicationPermissions(scenarioContext.getResource().toPublication(),
                                                         scenarioContext.getCurrentUserInstance());
        var expected = outcome.equals("Allowed");

        var actual = permissions.allowsAction(scenarioContext.getOperation());

        assertThat(actual, is(equalTo(expected)));
    }
}
