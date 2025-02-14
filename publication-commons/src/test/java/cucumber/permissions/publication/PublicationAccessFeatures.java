package cucumber.permissions.publication;

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
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.publication.model.business.Owner;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;

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
            scenarioContext.setCurrentUserAsFileCurator();
        }
        if (roles.contains(PUBLICATION_OWNER)) {
            scenarioContext.getResource().setResourceOwner(new Owner(scenarioContext.getCurrentUserInstance().getUser(), scenarioContext.getTopLevelOrgCristinId()));
        }
        if (roles.contains(PermissionsRole.OTHER_CONTRIBUTORS)) {
            scenarioContext.addCurrentUserAndTopLevelAsContributor();
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
