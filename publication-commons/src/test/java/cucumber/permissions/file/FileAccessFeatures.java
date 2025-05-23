package cucumber.permissions.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.FileOwner;
import cucumber.permissions.PermissionsRole;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.PublicationStatus;

public class FileAccessFeatures {

    private final FileScenarioContext scenarioContext;

    public FileAccessFeatures(FileScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a file of type {string}")
    public void aFileOfTheType(String fileType) throws ClassNotFoundException {
        scenarioContext.setFileType(fileType);
    }

    @Given("a file of type {string} with property {string}")
    public void aFileOfTheTypeAndWithProperty(String fileType, String fileProperty) throws ClassNotFoundException {
        scenarioContext.setFileType(fileType);
        scenarioContext.setIsDegree(fileProperty.toLowerCase().contains("degree"));
        scenarioContext.setIsEmbargo(fileProperty.toLowerCase().contains("embargo"));
    }

    @Given("a file of type {string} and publication status {string}")
    public void aFileOfTypeAndPublicationStatus(String fileType, String publicationStatus)
        throws ClassNotFoundException {
        scenarioContext.setFileType(fileType);
        scenarioContext.setPublicationStatus(PublicationStatus.valueOf(publicationStatus));
    }

    @When("the user have the role {string}")
    public void theUserHaveTheRole(String userRole) {
        scenarioContext.setRoles(PermissionsRole.lookup(userRole));
        scenarioContext.setFileBelongsToSameOrg(userRole.toLowerCase().contains("at x"));
    }

    @When("the file is owned by {string}")
    public void theFileIsOwnedBy(String fileOwner) {
        scenarioContext.setFileOwner(FileOwner.lookup(fileOwner));
    }

    @And("the user attempts to {string}")
    public void theUserAttemptsTo(String operation) {
        scenarioContext.setFileOperation(FileOperation.lookup(operation));
    }

    @Then("the action outcome is {string}")
    public void theActionOutcomeIs(String outcome) {
        var filePermissions = scenarioContext.getFilePermissions();
        var expected = outcome.equals("Allowed");

        var actual = filePermissions.allowsAction(scenarioContext.getFileOperation());

        assertThat("%s is %s to perform %s on %s".formatted(
                       scenarioContext.getRoles().stream().map(PermissionsRole::getValue).toList(),
                       outcome,
                       scenarioContext.getFileOperation(),
                       scenarioContext.getFileType().getSimpleName()),
                   actual,
                   is(equalTo(expected)));
    }

    @And("publication has claimed publisher")
    public void publicationHasClaimedPublisher() {
        scenarioContext.setHasClaimedPublisher(true);
    }

    @And("the user belongs to the organization that claimed the publisher")
    public void userBelongsToTheOrganizationThatClaimedThePublisher() {
        scenarioContext.setUserBelongsToOrganizationThatClaimedPublisher(true);
    }
}
