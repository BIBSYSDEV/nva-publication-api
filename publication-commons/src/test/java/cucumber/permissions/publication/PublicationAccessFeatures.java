package cucumber.permissions.publication;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.PermissionsRole;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.util.Arrays;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;

public class PublicationAccessFeatures {

    protected static final URI ORGANIZATION = randomUri();
    private final PublicationScenarioContext scenarioContext;

    public PublicationAccessFeatures(PublicationScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a {string} publication")
    public void aPublication(String publicationStatus) {
        scenarioContext.setPublicationStatus(PublicationStatus.lookup(publicationStatus));
    }

    @Given("a {string} publication with {string} property")
    public void aPublicationWithProperties(String publicationStatus, String properties) {
        scenarioContext.setPublicationStatus(PublicationStatus.lookup(publicationStatus));
        var propertyList = Arrays.stream(properties.split(",")).map(String::toLowerCase).toList();

        if (propertyList.contains("degree")) {
            scenarioContext.setIsDegree(true);
        }
        if (propertyList.contains("imported")) {
            scenarioContext.setIsImported(true);
        }
        if (propertyList.contains("metadataonly")) {
            scenarioContext.setIsMetadataOnly(true);
        }
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

        assertThat( "%s is %s to perform %s".formatted(scenarioContext.getRoles().stream().map(
                                                           PermissionsRole::getValue).toList(), outcome,
                                                       scenarioContext.getOperation()),
                    actual,
                    is(equalTo(expected)));
    }

    @And("publication is a degree")
    public void publicationIsADegree() {
        scenarioContext.setIsDegree(true);
    }

    @And("publication has claimed publisher")
    public void publicationHasClaimedPublisher() {
        scenarioContext.setHasClaimedPublisher(true);
    }

    @And("publisher is claimed by organization")
    public void publisherIsClaimedByOrganization() {
        scenarioContext.setPublisherOrganization(ORGANIZATION);
    }

    @And("the user is from the same organization as claimed publisher")
    public void theUserIsFromTheSameOrganizationAsClaimedPublisher() {
        scenarioContext.setUserOrganization(ORGANIZATION);
    }
}
