package cucumber.permissions.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.enums.ChannelClaimConfig;
import cucumber.permissions.enums.FileEmbargoConfig;
import cucumber.permissions.enums.FileOwnerConfig;
import cucumber.permissions.PermissionsRole;
import cucumber.permissions.enums.PublicationTypeConfig;
import cucumber.permissions.enums.UserInstitutionConfig;
import cucumber.permissions.publication.PublicationScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import no.unit.nva.model.FileOperation;
import no.unit.nva.model.PublicationStatus;

public class FileAccessFeatures {

    private final PublicationScenarioContext publicationScenarioContext;
    private final FileScenarioContext fileScenarioContext;

    public FileAccessFeatures(PublicationScenarioContext publicationScenarioContext,
                              FileScenarioContext fileScenarioContext) {
        this.publicationScenarioContext = publicationScenarioContext;
        this.fileScenarioContext = fileScenarioContext;
    }

    @Given("a file of type {string}")
    public void aFileOfType(String fileType) throws ClassNotFoundException {
        fileScenarioContext.setFileType(fileType);
    }

    @And("the file is owned by {string}")
    public void theFileIsOwnedBy(String fileOwner) {
        if ("user".equalsIgnoreCase(fileOwner)) {
            fileScenarioContext.setFileOwnerConfig(FileOwnerConfig.USER);
        } else if ("publication creator".equalsIgnoreCase(fileOwner)) {
            fileScenarioContext.setFileOwnerConfig(FileOwnerConfig.PUBLICATION_CREATOR);
        } else if ("contributor at curating institution".equalsIgnoreCase(fileOwner)) {
            fileScenarioContext.setFileOwnerConfig(FileOwnerConfig.CONTRIBUTOR_AT_CURATING_INSTITUTION);
        } else {
            throw new IllegalArgumentException("Non valid input: " + fileOwner);
        }
    }

    @And("the file has embargo")
    public void theFileHasEmbargo() {
        fileScenarioContext.setFileEmbargoConfig(FileEmbargoConfig.HAS_EMBARGO);
    }

    @And("publication is of type {string}")
    public void thePublicationIsOfType(String publication) {
        if ("publication".equalsIgnoreCase(publication)) {
            publicationScenarioContext.setPublicationTypeConfig(PublicationTypeConfig.PUBLICATION);
        } else if ("degree".equalsIgnoreCase(publication)) {
            publicationScenarioContext.setPublicationTypeConfig(PublicationTypeConfig.DEGREE);
        } else {
            throw new IllegalArgumentException("Non valid input: " + publication);
        }
    }

    @And("publication has status {string}")
    public void publicationHasStatus(String status) {
        publicationScenarioContext.setPublicationStatus(PublicationStatus.lookup(status));
    }

    @And("publication has publisher claimed by {string}")
    public void publicationHasPublisherClaimedBy(String claimedBy) {
        if ("users institution".equalsIgnoreCase(claimedBy)) {
            publicationScenarioContext.setChannelClaimConfig(ChannelClaimConfig.CLAIMED_BY_USERS_INSTITUTION);
        } else if ("not users institution".equalsIgnoreCase(claimedBy)) {
            publicationScenarioContext.setChannelClaimConfig(ChannelClaimConfig.CLAIMED_BY_NOT_USERS_INSTITUTION);
        } else {
            throw new IllegalArgumentException("Non valid input: " + claimedBy);
        }
    }

    @When("the user have the role {string}")
    public void theUserHaveTheRole(String userRole) {
        publicationScenarioContext.setRoles(PermissionsRole.lookup(userRole));
    }

    @And("the user belongs to {string}")
    public void theUserBelongTo(String institution) {
        if ("creating institution".equalsIgnoreCase(institution)) {
            publicationScenarioContext.setUserInstitutionConfig(UserInstitutionConfig.BELONGS_TO_CREATING_INSTITUTION);
        } else if ("curating institution".equalsIgnoreCase(institution)) {
            publicationScenarioContext.setUserInstitutionConfig(UserInstitutionConfig.BELONGS_TO_CURATING_INSTITUTION);
        } else if ("non curating institution".equalsIgnoreCase(institution)) {
            publicationScenarioContext.setUserInstitutionConfig(UserInstitutionConfig.BELONGS_TO_NON_CURATING_INSTITUTION);
        } else {
            throw new IllegalArgumentException("Non valid input: " + institution);
        }
    }

    @And("the user attempts to {string}")
    public void theUserAttemptsTo(String operation) {
        fileScenarioContext.setFileOperation(FileOperation.lookup(operation));
    }

    @Then("the action outcome is {string}")
    public void theActionOutcomeIs(String outcome) {
        var filePermissions = fileScenarioContext.getFilePermissions();
        var expected = outcome.equals("Allowed");

        var actual = filePermissions.allowsAction(fileScenarioContext.getFileOperation());

        assertThat("%s is %s to perform %s on %s".formatted(
                       publicationScenarioContext.getRoles().stream().map(PermissionsRole::getValue).toList(),
                       outcome,
                       fileScenarioContext.getFileOperation(),
                       fileScenarioContext.getFileClassFromString().getSimpleName()),
                   actual,
                   is(equalTo(expected)));
    }
}
