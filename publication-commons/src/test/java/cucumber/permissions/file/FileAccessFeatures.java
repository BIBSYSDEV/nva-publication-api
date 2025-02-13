package cucumber.permissions.file;

import static cucumber.permissions.PermissionsRole.FILE_CURATOR;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_FOR_GIVEN_FILE;
import static cucumber.permissions.PermissionsRole.FILE_OWNER;
import static java.time.temporal.ChronoUnit.DAYS;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.PermissionsRole;
import cucumber.permissions.file.FileScenarioContext.FileOwnership;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.permissions.file.FilePermissions;
import nva.commons.apigateway.AccessRight;

public class FileAccessFeatures {

    public static final String EMPTY_PROPERTY = "";
    private final FileScenarioContext scenarioContext;

    public FileAccessFeatures(FileScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a file of type {string}")
    public void aFileOfTheType(String string) throws ClassNotFoundException {
        scenarioContext.setResource(Resource.fromPublication(randomNonDegreePublication()));
        scenarioContext.setFile(createFile(string, EMPTY_PROPERTY));
    }

    @Given("a file of type {string} with property {string}")
    public void aFileOfTheTypeAndWithProperty(String fileType, String fileProperty) throws ClassNotFoundException {
        if (fileProperty.toLowerCase().contains("degree")) {
            scenarioContext.setResource(Resource.fromPublication(randomDegreePublication()));
        } else {
            scenarioContext.setResource(Resource.fromPublication(randomNonDegreePublication()));
        }

        scenarioContext.setFile(createFile(fileType, fileProperty));
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
        if (roles.contains(FILE_OWNER) || roles.contains(FILE_CURATOR_FOR_GIVEN_FILE)) {
            scenarioContext.setFileOwnership(FileOwnership.OWNER);
        } else {
            scenarioContext.setFileOwnership(FileOwnership.NOT_OWNER);
        }
    }

    @And("the user attempts to {string}")
    public void theUserAttemptsTo(String operation) {
        scenarioContext.setFileOperation(FileOperation.lookup(operation));
    }

    @Then("the action outcome is {string}")
    public void theActionOutcomeIs(String outcome) {
        var filePermissions = new FilePermissions(scenarioContext.getFileEntry(),
                                                  scenarioContext.getCurrentUserInstance(),
                                                  scenarioContext.getResource());
        var expected = outcome.equals("Allowed");

        var actual = filePermissions.allowsAction(scenarioContext.getFileOperation());

        assertThat(actual, is(equalTo(expected)));
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
}
