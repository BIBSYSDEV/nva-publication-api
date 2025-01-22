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

public class FileAccessFeatures {

    public static final String EMPTY_PROPERTY = "";
    private final FileScenarioContext scenarioContext;

    public FileAccessFeatures(FileScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a file of type {string}")
    public void aFileOfTheType(String string) throws ClassNotFoundException {
        var file = createFile(string, EMPTY_PROPERTY);
        scenarioContext.setPublication(randomNonDegreePublication());
        scenarioContext.setFile(file);
    }

    @Given("a file of type {string} with property {string}")
    public void aFileOfTheTypeAndWithProperty(String fileType, String fileProperty) throws ClassNotFoundException {
        var file = createFile(fileType, fileProperty);
        if (fileProperty.toLowerCase().contains("degree")) {
            scenarioContext.setPublication(randomDegreePublication());
        } else {
            scenarioContext.setPublication(randomNonDegreePublication());
        }
        scenarioContext.setFile(file);
    }

    @SuppressWarnings("unchecked")
    private static File createFile(String fileType, String fileProperty) throws ClassNotFoundException {
        var clazz = (Class<File>) Class.forName(File.class.getPackageName() + "." + fileType);
        var file = File.builder();
        if (fileProperty.toLowerCase().contains("embargo")) {
            file.withEmbargoDate(Instant.now().plus(100, DAYS));
        }
        return file.build(clazz);
    }

    @When("the user have the role {string}")
    public void theUserHaveTheRole(String useraRole) {
        var user = UserInstance.create(randomString(), randomUri());
        scenarioContext.setUser(user);
    }

    @And("the user attempts to {string}")
    public void theUserAttemptsTo(String operation) {
        scenarioContext.setFileOperation(FileOperation.lookup(operation));
    }

    @Then("the action outcome is {string}")
    public void theActionOutcomeIs(String outcome) {
        var permissionStrategy = new FilePermissions(scenarioContext.getFile(), scenarioContext.getUser(),
                                                     scenarioContext.getPublication());
        var expected = outcome.equals("Allowed");

        var actual = permissionStrategy.allowsAction(scenarioContext.getFileOperation());

        assertThat(actual, is(equalTo(expected)));
    }
}
