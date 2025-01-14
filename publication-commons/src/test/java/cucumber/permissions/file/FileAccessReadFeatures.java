package cucumber.permissions.file;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissionStrategy;

public class FileAccessReadFeatures {
    private final FileScenarioContext scenarioContext;

    public FileAccessReadFeatures(FileScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @SuppressWarnings("unchecked")
    @Given("a file in the {string} state")
    public void a_file_in_the_state(String string) throws ClassNotFoundException {
        var clazz = (Class<File>)Class.forName(File.class.getPackageName() + "." + string);
        var file = File.builder().build(clazz);
        scenarioContext.setFile(file);
    }

    @When("a user with the role {string}")
    public void a_user_with_the_role(String userRole) {
        var user = UserInstance.create(randomString(), randomUri());
        scenarioContext.setUser(user);
    }

    @When("the user attempts to read-metadata")
    public void user_attempts_to_read_metadata() {
        scenarioContext.setFileOperation(FileOperation.lookup("read-metadata"));
    }

    @And("the user attempts to upload a file")
    public void user_attempts_to_upload() {
        // TODO
    }

    @And("the user attempts to edit or delete the file")
    public void user_attempts_to_write() {
        // TODO
    }

    @Then("the action outcome is {string}")
    public void theNvaResourceHasAPublicationContextWithPublisherWithNameEqualTo(String outcome) {
        var permissionStrategy = new FilePermissionStrategy(scenarioContext.getFile(), scenarioContext.getUser());
        var expected = outcome.equals("Allowed");

        var actual = permissionStrategy.allowsAction(scenarioContext.getFileOperation());

        assertThat(actual, is(equalTo(expected)));
    }
}
