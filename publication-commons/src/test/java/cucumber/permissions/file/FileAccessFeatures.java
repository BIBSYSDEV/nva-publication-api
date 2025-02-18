package cucumber.permissions.file;

import static cucumber.permissions.PermissionsRole.UNAUTHENTICATED;
import static cucumber.permissions.PermissionsRole.EXTERNAL_CLIENT;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_DEGREE;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_DEGREE_EMBARGO;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_FOR_GIVEN_FILE;
import static cucumber.permissions.PermissionsRole.FILE_CURATOR_FOR_OTHERS;
import static cucumber.permissions.PermissionsRole.OTHER_CONTRIBUTORS;
import static cucumber.permissions.PermissionsRole.FILE_OWNER;
import static java.time.temporal.ChronoUnit.DAYS;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.PermissionsRole;
import cucumber.permissions.file.FileScenarioContext.FileRelationship;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Instant;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserClientType;
import no.unit.nva.publication.permissions.file.FilePermissions;

public class FileAccessFeatures {

    public static final String EMPTY_PROPERTY = "";
    private final FileScenarioContext scenarioContext;

    public FileAccessFeatures(FileScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a file of type {string}")
    public void aFileOfTheType(String string) throws ClassNotFoundException {
        scenarioContext.setResource(
            Resource.fromPublication(randomNonDegreePublication().copy().withStatus(PUBLISHED).build()));
        scenarioContext.setFile(createFile(string, EMPTY_PROPERTY));
    }

    @Given("a file of type {string} with property {string}")
    public void aFileOfTheTypeAndWithProperty(String fileType, String fileProperty) throws ClassNotFoundException {
        if (fileProperty.toLowerCase().contains("degree")) {
            scenarioContext.setResource(
                Resource.fromPublication(randomDegreePublication().copy().withStatus(PUBLISHED).build()));
        } else {
            scenarioContext.setResource(
                Resource.fromPublication(randomNonDegreePublication().copy().withStatus(PUBLISHED).build()));
        }

        scenarioContext.setFile(createFile(fileType, fileProperty));
    }

    @Given("a file of type {string} and publication status {string}")
    public void aFileOfTypeAndPublicationStatus(String fileType, String publicationStatus)
        throws ClassNotFoundException {
        scenarioContext.setResource(Resource.fromPublication(randomNonDegreePublication())
                                        .copy()
                                        .withStatus(PublicationStatus.valueOf(publicationStatus))
                                        .build());
        scenarioContext.setFile(createFile(fileType, EMPTY_PROPERTY));
    }

    @When("the user have the role {string}")
    public void theUserHaveTheRole(String userRole) {
        var roles = PermissionsRole.lookup(userRole);

        if (roles.contains(FILE_OWNER)) {
            scenarioContext.setFileRelationship(FileRelationship.OWNER);
        } else {
            scenarioContext.setFileRelationship(FileRelationship.NO_RELATION);
        }

        if (roles.contains(FILE_CURATOR_FOR_OTHERS)) {
            scenarioContext.setCurrentUserAsFileCurator();
        }

        if (roles.contains(UNAUTHENTICATED)) {
            scenarioContext.setCurrentUserAsNotAuthenticated();
        }

        if (roles.contains(FILE_CURATOR_DEGREE_EMBARGO)) {
            scenarioContext.setCurrentUserAsDegreeEmbargoFileCuratorForGivenFile();
        }

        if (roles.contains(FILE_CURATOR_DEGREE)) {
            scenarioContext.setCurrentUserAsDegreeFileCuratorForGivenFile();
        }

        if (roles.contains(FILE_CURATOR_FOR_GIVEN_FILE)) {
            scenarioContext.setCurrentUserAsFileCuratorForGivenFile();
        }

        if (roles.contains(OTHER_CONTRIBUTORS)) {
            scenarioContext.addCurrentUserAndTopLevelAsContributor();
        }

        if (roles.contains(EXTERNAL_CLIENT)) {
            scenarioContext.setUserClientType(UserClientType.EXTERNAL);
            scenarioContext.setPublisherId(scenarioContext.getCurrentUserInstance().getCustomerId());
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
