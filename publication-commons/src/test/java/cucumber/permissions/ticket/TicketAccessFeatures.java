package cucumber.permissions.ticket;

import static cucumber.permissions.enums.TicketTypeConfig.DOI_REQUEST;
import static cucumber.permissions.enums.TicketTypeConfig.FILE_APPROVAL;
import static cucumber.permissions.enums.TicketTypeConfig.SUPPORT_REQUEST;
import static cucumber.permissions.publication.PublicationScenarioContext.CURATING_INSTITUTION;
import static cucumber.permissions.publication.PublicationScenarioContext.NON_CURATING_INSTITUTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import cucumber.permissions.PermissionsRole;
import cucumber.permissions.enums.ChannelClaimConfig;
import cucumber.permissions.enums.PublicationFileConfig;
import cucumber.permissions.enums.PublicationTypeConfig;
import cucumber.permissions.enums.TicketOwnerConfig;
import cucumber.permissions.enums.TicketStatusConfig;
import cucumber.permissions.enums.UserInstitutionConfig;
import cucumber.permissions.publication.PublicationScenarioContext;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.publicationchannel.ChannelPolicy;

public class TicketAccessFeatures {

    private final PublicationScenarioContext publicationScenarioContext;
    private final TicketScenarioContext ticketScenarioContext;

    public TicketAccessFeatures(PublicationScenarioContext publicationScenarioContext, TicketScenarioContext ticketScenarioContext) {
        this.publicationScenarioContext = publicationScenarioContext;
        this.ticketScenarioContext = ticketScenarioContext;
    }

    @Given("a {string}")
    public void a(String publication) {
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

    @And("publication has {string} files")
    public void publicationHasFiles(String fileTypes) {
        if ("no".equalsIgnoreCase(fileTypes)) {
            publicationScenarioContext.setPublicationFileConfig(PublicationFileConfig.NO_FILES);
        } else if ("no finalized".equalsIgnoreCase(fileTypes)) {
            publicationScenarioContext.setPublicationFileConfig(PublicationFileConfig.NON_FINALIZED_FILES);
        } else if ("finalized".equalsIgnoreCase(fileTypes)) {
            publicationScenarioContext.setPublicationFileConfig(PublicationFileConfig.FINALIZED_FILES);
        } else {
            throw new IllegalArgumentException("Non valid input: " + fileTypes);
        }
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

    @And("channel claim has {string} policy {string}")
    public void channelClaimHasPublishingPolicy(String policyType, String policyValue) {
        //        var policy = ChannelPolicy.valueOf(policyValue);
        var policy = switch (policyValue.toLowerCase()) {
            case "owneronly" -> ChannelPolicy.OWNER_ONLY;
            case "everyone" -> ChannelPolicy.EVERYONE;
            default -> throw new IllegalArgumentException("Non valid input: " + policyValue);
        };
        if ("publishing".equalsIgnoreCase(policyType)) {
            publicationScenarioContext.setChannelClaimPublishingPolicy(policy);
        } else if ("editing".equalsIgnoreCase(policyType)) {
            publicationScenarioContext.setChannelClaimEditingPolicy(policy);
        } else {
            throw new IllegalArgumentException("Non valid input: " + policyType);
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

    @And("the ticket receiver is {string}")
    public void theTicketReceiverIs(String ticketReceiver) {
        if ("users institution".equalsIgnoreCase(ticketReceiver)) {
            ticketScenarioContext.setTicketReceiver(publicationScenarioContext.getUserInstance().getTopLevelOrgCristinId());
        } else if ("curating institution".equalsIgnoreCase(ticketReceiver)) {
            ticketScenarioContext.setTicketReceiver(CURATING_INSTITUTION);
        } else if ("non curating institution".equalsIgnoreCase(ticketReceiver)) {
            ticketScenarioContext.setTicketReceiver(NON_CURATING_INSTITUTION);
        } else {
            throw new IllegalArgumentException("Non valid input: " + ticketReceiver);
        }
    }

    @And("ticket creator is {string}")
    public void ticketCreatorIs(String input) {
        if ("user".equalsIgnoreCase(input)) {
            ticketScenarioContext.setTicketCreator(TicketOwnerConfig.USER);
        } else if ("other user at different institution".equalsIgnoreCase(input)) {
            ticketScenarioContext.setTicketCreator(TicketOwnerConfig.OTHER_USER_AT_DIFFERENT_INSTITUTION);
        } else if ("other user at same institution".equalsIgnoreCase(input)) {
            ticketScenarioContext.setTicketCreator(TicketOwnerConfig.OTHER_USER_AT_SAME_INSTITUTION);
        } else {
            throw new IllegalArgumentException("Non valid input: " + input);
        }
    }

    @And("the user attempts to {string}")
    public void theUserAttemptsTo(String operation) {
        ticketScenarioContext.setOperation(TicketOperation.lookup(operation));
    }

    @Then("the action outcome is {string}")
    public void theActionOutcomeIs(String outcome) {
        var permissions = ticketScenarioContext.getTicketPermissions();

        var expected = outcome.equals("Allowed");

        var actual = permissions.allowsAction(ticketScenarioContext.getOperation());

        assertThat( "%s is %s to perform %s".formatted(publicationScenarioContext.getRoles().stream().map(
                                                           PermissionsRole::getValue).toList(), outcome,
                                                       ticketScenarioContext.getOperation()),
                    actual,
                    is(equalTo(expected)));
    }

    @And("ticket is of type {string}")
    public void ticketIsOfType(String input) {
        if ("file approval".equalsIgnoreCase(input)) {
            ticketScenarioContext.setTicketType(FILE_APPROVAL);
        } else if ("doi request".equalsIgnoreCase(input)) {
            ticketScenarioContext.setTicketType(DOI_REQUEST);
        } else if ("support request".equalsIgnoreCase(input)) {
            ticketScenarioContext.setTicketType(SUPPORT_REQUEST);
        } else {
            throw new IllegalArgumentException("Non valid input: " + input);
        }
    }

    @And("ticket status is {string}")
    public void ticketStatusIs(String input) {
        if ("completed".equalsIgnoreCase(input)) {
            ticketScenarioContext.setTicketStatus(TicketStatusConfig.COMPLETED);
        } else if ("pending".equalsIgnoreCase(input)) {
            ticketScenarioContext.setTicketStatus(TicketStatusConfig.PENDING);
        } else {
            throw new IllegalArgumentException("Non valid input: " + input);
        }
    }
}
