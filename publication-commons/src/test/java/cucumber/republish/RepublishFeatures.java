package cucumber.republish;

import static java.util.Collections.emptySet;
import static java.util.stream.IntStream.range;
import static no.unit.nva.model.PublicationStatus.PUBLISHED;
import static no.unit.nva.model.PublicationStatus.UNPUBLISHED;
import static no.unit.nva.model.testing.PublicationGenerator.randomPublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static no.unit.nva.publication.model.business.TicketStatus.NOT_APPLICABLE;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
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
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.publication.commons.customer.Customer;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.ticket.test.TicketTestUtils;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class RepublishFeatures {

  private final RepublishScenarioContext scenarioContext;
  private Username assignedCurator;

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

    persistPendingFiles(institution, fileCount);
  }

  @Given("institution {string} has {int} pending file(s) without an approval ticket")
  public void institutionHasPendingFilesWithoutAnApprovalTicket(String institution, int fileCount) {
    persistPendingFiles(institution, fileCount);
  }

  @Given("institution {string} has {int} pending file(s) with an approval ticket")
  public void institutionHasPendingFilesWithAnApprovalTicket(String institution, int fileCount)
      throws ApiGatewayException {
    var uploader = scenarioContext.uploaderAt(institution);
    var files = persistPendingFiles(institution, fileCount);
    var resource = scenarioContext.currentResource();

    var ticket =
        PublishingRequestCase.createWithFilesForApproval(
                resource, uploader, REGISTRATOR_PUBLISHES_METADATA_ONLY, Set.copyOf(files))
            .persistNewTicket(scenarioContext.ticketService(), resource.toPublication());
    scenarioContext.setOriginalTicket(institution, ticket);
  }

  @Given("the approval ticket of institution {string} is assigned to a curator")
  public void theApprovalTicketOfInstitutionIsAssignedToACurator(String institution) {
    assignedCurator = new Username(randomString());
    var ticket = scenarioContext.originalTicket(institution);
    ticket.setAssignee(assignedCurator);
    scenarioContext.ticketService().updateTicket(ticket);
  }

  @Given("a file from institution {string} is removed while the publication is unpublished")
  public void aFileFromInstitutionIsRemovedWhileThePublicationIsUnpublished(String institution) {
    fileEntryOf(scenarioContext.filesOf(institution).getFirst())
        .softDelete(scenarioContext.resourceService(), new User(randomString()));
  }

  @Given("a file from institution {string} gets a new license while the publication is unpublished")
  public void aFileFromInstitutionGetsANewLicenseWhileThePublicationIsUnpublished(
      String institution) {
    var fileEntry = fileEntryOf(scenarioContext.filesOf(institution).getFirst());
    var fileWithNewLicense =
        fileEntry.getFile().copy().withLicense(randomUri()).buildPendingOpenFile();
    fileEntry.update(
        fileWithNewLicense,
        scenarioContext.uploaderAt(institution),
        scenarioContext.resourceService());
  }

  @Given("the publication is changed into a degree while unpublished")
  public void thePublicationIsChangedIntoADegreeWhileUnpublished() {
    var resource = scenarioContext.currentResource();
    var degreeReference = randomPublication(DegreePhd.class).getEntityDescription().getReference();
    resource.getEntityDescription().setReference(degreeReference);
    scenarioContext.resourceService().updateResource(resource, scenarioContext.publicationOwner());
  }

  @Given("the customer publishes files without curator approval")
  public void theCustomerPublishesFilesWithoutCuratorApproval() {
    var autoPublishingCustomer =
        new Customer(emptySet(), REGISTRATOR_PUBLISHES_METADATA_AND_FILES.getValue(), null);
    scenarioContext
        .customerApiClient()
        .withCustomer(scenarioContext.publication().getPublisher().getId(), autoPublishingCustomer);
  }

  @When("the publication is republished")
  public void thePublicationIsRepublished() {
    Resource.resourceQueryObject(scenarioContext.publication().getIdentifier())
        .republish(
            scenarioContext.resourceService(),
            scenarioContext.customerApiClient(),
            scenarioContext.publicationOwner());
  }

  @Then("institution {string} has a pending file approval ticket covering {int} file(s)")
  public void institutionHasAPendingFileApprovalTicketCoveringFiles(
      String institution, int fileCount) {
    var tickets = pendingTicketsOwnedBy(scenarioContext.institutionUri(institution));

    assertThat(tickets).hasSize(1);
    assertThat(tickets.getFirst().getFilesForApproval()).hasSize(fileCount);
  }

  @Then("institution {string} has a pending degree file approval ticket covering {int} file(s)")
  public void institutionHasAPendingDegreeFileApprovalTicketCoveringFiles(
      String institution, int fileCount) {
    var tickets = pendingTicketsOwnedBy(scenarioContext.institutionUri(institution));

    assertThat(tickets).hasSize(1);
    assertThat(tickets.getFirst()).isInstanceOf(FilesApprovalThesis.class);
    assertThat(tickets.getFirst().getFilesForApproval()).hasSize(fileCount);
  }

  @Then("institution {string} has a completed file approval ticket approving {int} file(s)")
  public void institutionHasACompletedFileApprovalTicketApprovingFiles(
      String institution, int fileCount) {
    var completedTickets =
        ticketsOwnedBy(scenarioContext.institutionUri(institution)).stream()
            .filter(ticket -> COMPLETED == ticket.getStatus())
            .toList();

    assertThat(completedTickets).hasSize(1);
    assertThat(completedTickets.getFirst().getApprovedFiles()).hasSize(fileCount);
  }

  @Then(
      "the original approval ticket of institution {string} is pending again with the same curator")
  public void theOriginalApprovalTicketOfInstitutionIsPendingAgainWithTheSameCurator(
      String institution) {
    var ticket = scenarioContext.originalTicket(institution);

    assertThat(ticket.getStatus()).isEqualTo(PENDING);
    assertThat(ticket.getAssignee()).isEqualTo(assignedCurator);
  }

  @Then("the original approval ticket of institution {string} is set aside")
  public void theOriginalApprovalTicketOfInstitutionIsSetAside(String institution) {
    assertThat(scenarioContext.originalTicket(institution).getStatus()).isEqualTo(NOT_APPLICABLE);
  }

  @And("the publication has {int} pending file approval ticket(s)")
  public void thePublicationHasPendingFileApprovalTickets(int ticketCount) {
    assertThat(scenarioContext.pendingFileApprovalTickets()).hasSize(ticketCount);
  }

  private List<File> persistPendingFiles(String institution, int fileCount) {
    var uploader = scenarioContext.uploaderAt(institution);
    var files = range(0, fileCount).mapToObj(ignored -> persistPendingFile(uploader)).toList();
    scenarioContext.addFiles(institution, files);
    return files;
  }

  private File persistPendingFile(UserInstance uploader) {
    var file = randomPendingOpenFile();
    FileEntry.create(file, scenarioContext.publication().getIdentifier(), uploader)
        .persist(scenarioContext.resourceService(), uploader);
    return file;
  }

  private FileEntry fileEntryOf(File file) {
    return FileEntry.queryObject(
            file.getIdentifier(), scenarioContext.publication().getIdentifier())
        .fetch(scenarioContext.resourceService())
        .orElseThrow();
  }

  private List<FilesApprovalEntry> ticketsOwnedBy(URI institution) {
    return scenarioContext.fileApprovalTickets().stream()
        .filter(ticket -> institution.equals(ticket.getOwnerAffiliation()))
        .toList();
  }

  private List<FilesApprovalEntry> pendingTicketsOwnedBy(URI institution) {
    return ticketsOwnedBy(institution).stream().filter(FilesApprovalEntry::isPending).toList();
  }
}
