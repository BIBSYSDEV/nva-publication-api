package cucumber.republish;

import static java.util.Collections.emptySet;
import static java.util.stream.IntStream.range;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.util.List;
import java.util.Set;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class RepublishFeatures {

  private final RepublishScenarioContext scenarioContext;

  public RepublishFeatures(RepublishScenarioContext scenarioContext) {
    this.scenarioContext = scenarioContext;
  }

  @Before
  public void startScenario() {
    scenarioContext.startScenario();
  }

  @After
  public void endScenario() {
    scenarioContext.endScenario();
  }

  @Given("a published publication")
  public void aPublishedPublication() throws ApiGatewayException {
    var publication =
        TicketTestUtils.createPersistedPublicationWithAssociatedLink(
            PUBLISHED, scenarioContext.resourceService());
    scenarioContext.setPublication(publication);
  }

  @Given("the publication is unpublished")
  public void thePublicationIsUnpublished() throws ApiGatewayException {
    scenarioContext
        .resourceService()
        .unpublishPublication(
            scenarioContext.currentResource().toPublication(), scenarioContext.publicationOwner());
  }

  @Given("institution {string} uploads {int} file(s) while the publication is unpublished")
  public void institutionUploadsFilesWhileThePublicationIsUnpublished(
      String institution, int fileCount) {
    assertThat(scenarioContext.currentResource().getStatus()).isEqualTo(UNPUBLISHED);

    persistPendingFiles(scenarioContext.uploaderAt(institution), fileCount);
  }

  @Given("institution {string} has {int} pending file(s) without an approval ticket")
  public void institutionHasPendingFilesWithoutAnApprovalTicket(String institution, int fileCount) {
    persistPendingFiles(scenarioContext.uploaderAt(institution), fileCount);
  }

  @Given("institution {string} has {int} pending file(s) with an approval ticket")
  public void institutionHasPendingFilesWithAnApprovalTicket(String institution, int fileCount)
      throws ApiGatewayException {
    var uploader = scenarioContext.uploaderAt(institution);
    var files = persistPendingFiles(uploader, fileCount);
    var resource = scenarioContext.currentResource();

    PublishingRequestCase.createWithFilesForApproval(
            resource, uploader, REGISTRATOR_PUBLISHES_METADATA_ONLY, Set.copyOf(files))
        .persistNewTicket(scenarioContext.ticketService(), resource.toPublication());
  }

  @When("the publication is republished")
  public void thePublicationIsRepublished() {
    Resource.resourceQueryObject(scenarioContext.publication().getIdentifier())
        .republish(
            scenarioContext.resourceService(),
            scenarioContext.customerApiClient(),
            scenarioContext.publicationOwner());
  }

  @Then("institution {string} has a file approval ticket covering {int} file(s)")
  public void institutionHasAFileApprovalTicketCoveringFiles(String institution, int fileCount) {
    var tickets = ticketsOwnedBy(scenarioContext.institutionUri(institution));

    assertThat(tickets).hasSize(1);
    assertThat(tickets.getFirst().getFilesForApproval()).hasSize(fileCount);
  }

  @Given("the customer publishes files without curator approval")
  public void theCustomerPublishesFilesWithoutCuratorApproval() {
    var autoPublishingCustomer =
        new Customer(emptySet(), REGISTRATOR_PUBLISHES_METADATA_AND_FILES.getValue(), null);
    scenarioContext
        .customerApiClient()
        .withCustomer(scenarioContext.publication().getPublisher().getId(), autoPublishingCustomer);
  }

  @Then("institution {string} has a completed file approval ticket approving {int} file(s)")
  public void institutionHasACompletedFileApprovalTicketApprovingFiles(
      String institution, int fileCount) {
    var tickets = ticketsOwnedBy(scenarioContext.institutionUri(institution));

    assertThat(tickets).hasSize(1);
    assertThat(tickets.getFirst().getStatus()).isEqualTo(COMPLETED);
    assertThat(tickets.getFirst().getApprovedFiles()).hasSize(fileCount);
  }

  @And("the publication has {int} file approval ticket(s)")
  public void thePublicationHasFileApprovalTickets(int ticketCount) {
    assertThat(scenarioContext.fileApprovalTickets()).hasSize(ticketCount);
  }

  private List<File> persistPendingFiles(UserInstance uploader, int fileCount) {
    return range(0, fileCount).mapToObj(ignored -> persistPendingFile(uploader)).toList();
  }

  private File persistPendingFile(UserInstance uploader) {
    var file = randomPendingOpenFile();
    FileEntry.create(file, scenarioContext.publication().getIdentifier(), uploader)
        .persist(scenarioContext.resourceService(), uploader);
    return file;
  }

  private List<FilesApprovalEntry> ticketsOwnedBy(URI institution) {
    return scenarioContext.fileApprovalTickets().stream()
        .filter(ticket -> institution.equals(ticket.getOwnerAffiliation()))
        .toList();
  }
}
