package cucumber.permissions.file;

import static java.time.temporal.ChronoUnit.DAYS;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.file.FilePermissions;

public class FileAccessReadFeatures {

    public static final String EMPTY_PROPERTY = "";
    private final FileScenarioContext scenarioContext;

    public FileAccessReadFeatures(FileScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a file of type {string}")
    public void a_file_in_the_state(String string) throws ClassNotFoundException {
        var file = getFile(string, EMPTY_PROPERTY);
        scenarioContext.setPublication(randomNonDegreePublication());
        scenarioContext.setFile(file);
    }

    @Given("a file of type {string} with property {string}")
    public void a_file_in_the_state(String fileType, String fileProperty) throws ClassNotFoundException {
        var file = getFile(fileType, fileProperty);
        if (fileProperty.toLowerCase().contains("degree")) {
            scenarioContext.setPublication(randomDegreePublication());
        } else {
            scenarioContext.setPublication(randomNonDegreePublication());
        }
        scenarioContext.setFile(file);
    }

    @SuppressWarnings("unchecked")
    private static File getFile(String fileType, String fileProperty) throws ClassNotFoundException {
        var clazz = (Class<File>)Class.forName(File.class.getPackageName() + "." + fileType);
        var file = File.builder();
        if (fileProperty.toLowerCase().contains("embargo")) {
            file.withEmbargoDate(Instant.now().plus(100, DAYS));
        }
        return file.build(clazz);
    }

    @When("a user have the role {string}")
    public void a_user_with_the_role(String useraRole) {
        var user = UserInstance.create(randomString(), randomUri());
        scenarioContext.setUser(user);
    }

    @And("the user attempts to {string}")
    public void user_attempts_to(String string) {
        scenarioContext.setFileOperation(FileOperation.lookup(string));
    }

    @Then("the action outcome is {string}")
    public void theNvaResourceHasAPublicationContextWithPublisherWithNameEqualTo(String outcome) {
        var permissionStrategy = new FilePermissions(scenarioContext.getFile(), scenarioContext.getUser(),
                                                     scenarioContext.getPublication());
        var expected = outcome.equals("Allowed");

        var actual = permissionStrategy.allowsAction(scenarioContext.getFileOperation());

        assertThat(actual, is(equalTo(expected)));
    }
}
