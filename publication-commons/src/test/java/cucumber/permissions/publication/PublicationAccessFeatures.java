package cucumber.permissions.publication;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.PermissionsRole;
import cucumber.permissions.enums.ChannelClaimConfig;
import cucumber.permissions.enums.PublicationFileConfig;
import cucumber.permissions.enums.PublicationTypeConfig;
import cucumber.permissions.enums.UserInstitutionConfig;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import no.unit.nva.model.PublicationOperation;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;

public class PublicationAccessFeatures {

    private final PublicationScenarioContext scenarioContext;

    public PublicationAccessFeatures(PublicationScenarioContext scenarioContext) {
        this.scenarioContext = scenarioContext;
    }

    @Given("a {string}")
    public void a(String publication) {
        if ("publication".equalsIgnoreCase(publication)) {
            scenarioContext.setPublicationTypeConfig(PublicationTypeConfig.PUBLICATION);
        } else if ("degree".equalsIgnoreCase(publication)) {
            scenarioContext.setPublicationTypeConfig(PublicationTypeConfig.DEGREE);
        } else {
            throw new IllegalArgumentException("Non valid input: " + publication);
        }
    }

    @And("publication has status {string}")
    public void publicationHasStatus(String status) {
        scenarioContext.setPublicationStatus(PublicationStatus.lookup(status));
    }

    @And("publication has {string} files")
    public void publicationHasFiles(String fileTypes) {
        if ("no".equalsIgnoreCase(fileTypes)) {
            scenarioContext.setPublicationFileConfig(PublicationFileConfig.NO_FILES);
        } else if ("no finalized".equalsIgnoreCase(fileTypes)) {
            scenarioContext.setPublicationFileConfig(PublicationFileConfig.NON_FINALIZED_FILES);
        } else if ("finalized".equalsIgnoreCase(fileTypes)) {
            scenarioContext.setPublicationFileConfig(PublicationFileConfig.FINALIZED_FILES);
        } else {
            throw new IllegalArgumentException("Non valid input: " + fileTypes);
        }
    }

    @And("publication has publisher claimed by {string}")
    public void publicationHasPublisherClaimedBy(String claimedBy) {
        if ("users institution".equalsIgnoreCase(claimedBy)) {
            scenarioContext.setChannelClaimConfig(ChannelClaimConfig.CLAIMED_BY_USERS_INSTITUTION);
        } else if ("not users institution".equalsIgnoreCase(claimedBy)) {
            scenarioContext.setChannelClaimConfig(ChannelClaimConfig.CLAIMED_BY_NOT_USERS_INSTITUTION);
        } else {
            throw new IllegalArgumentException("Non valid input: " + claimedBy);
        }
    }

    @And("channel claim has {string} policy {string}")
    public void channelClaimHasPublishingPolicy(String policyType, String policyValue) {
//        var policy = ChannelPolicy.valueOf(policyValue);
        var policy = switch (policyValue.toLowerCase()) {
            case "owneronly" -> ChannelPolicy.OWNER_ONLY;
            case "everyone" -> ChannelPolicy.EVERYONE;
            default -> throw new IllegalArgumentException("Non valid input: " + policyValue);
        };
        if ("publishing".equalsIgnoreCase(policyType)) {
            scenarioContext.setChannelClaimPublishingPolicy(policy);
        } else if ("editing".equalsIgnoreCase(policyType)) {
            scenarioContext.setChannelClaimEditingPolicy(policy);
        } else {
            throw new IllegalArgumentException("Non valid input: " + policyType);
        }
    }

    @And("publication is an imported degree")
    public void publicationIsAnImportedDegree() {
        scenarioContext.setPublicationTypeConfig(PublicationTypeConfig.DEGREE);
        scenarioContext.setIsImportedDegree(true);
    }

    @When("the user have the role {string}")
    public void theUserHaveTheRole(String userRole) {
        scenarioContext.setRoles(PermissionsRole.lookup(userRole));
    }

    @And("the user belongs to {string}")
    public void theUserBelongTo(String institution) {
        if ("creating institution".equalsIgnoreCase(institution)) {
            scenarioContext.setUserInstitutionConfig(UserInstitutionConfig.BELONGS_TO_CREATING_INSTITUTION);
        } else if ("curating institution".equalsIgnoreCase(institution)) {
            scenarioContext.setUserInstitutionConfig(UserInstitutionConfig.BELONGS_TO_CURATING_INSTITUTION);
        } else if ("non curating institution".equalsIgnoreCase(institution)) {
            scenarioContext.setUserInstitutionConfig(UserInstitutionConfig.BELONGS_TO_NON_CURATING_INSTITUTION);
        } else {
            throw new IllegalArgumentException("Non valid input: " + institution);
        }
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

        assertThat("%s is %s to perform %s".formatted(
                       scenarioContext.getRoles().stream().map(PermissionsRole::getValue).toList(),
                       outcome,
                       scenarioContext.getOperation()),
                   actual,
                   is(equalTo(expected)));
    }
}
