package cucumber.permissions.publication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.PermissionsRole;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;

public class PublicationAccessFeatures {

    private final PublicationScenarioContext scenarioContext;

    public PublicationAccessFeatures(PublicationScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a {string} publication")
    public void aPublication(String publicationStatus) {
        scenarioContext.setPublicationStatus(PublicationStatus.lookup(publicationStatus));
    }

    @When("the user have the role {string}")
    public void theUserHaveTheRole(String userRole) {
        scenarioContext.setRoles(PermissionsRole.lookup(userRole));
    }

    @And("the user attempts to {string}")
    public void theUserAttemptsTo(String operation) {
        scenarioContext.setOperation(PublicationOperation.lookup(operation));
    }

    @Then("the action outcome is {string}")
    public void theActionOutcomeIs(String outcome) {
        var permissions = scenarioContext.getPublicationPermissions();

        var expected = outcome.equals("Allowed");

        var actual = permissions.allowsAction(scenarioContext.getOperation());

        assertThat(actual, is(equalTo(expected)));
    }
}
