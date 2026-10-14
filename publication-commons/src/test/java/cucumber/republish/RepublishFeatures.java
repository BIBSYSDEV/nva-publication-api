package cucumber.republish;

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
import static no.unit.nva.publication.service.CustomerGenerator.customerWithWorkflow;
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
import java.util.function.Supplier;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.instancetypes.degree.DegreePhd;
import no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.RepublishingService;
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

    persistPendingFiles(institution, fileCount);
  }

  @Given(
      "a user from institution {string} at another customer uploads {int} file(s) while the"
          + " publication is unpublished")
  public void aUserFromInstitutionAtAnotherCustomerUploadsFilesWhileThePublicationIsUnpublished(
      String institution, int fileCount) {
    var owner =
        new ResourceOwner(
            new Username(randomString()), scenarioContext.institutionUri(institution));
    var uploaderAtOtherCustomer = UserInstance.create(owner, randomUri());
    range(0, fileCount)
        .forEach(ignored -> persistFile(uploaderAtOtherCustomer, randomPendingOpenFile()));
  }

  @Given("a file without an uploader institution is uploaded while the publication is unpublished")
  public void aFileWithoutAnUploaderInstitutionIsUploadedWhileThePublicationIsUnpublished() {
    var uploaderWithoutInstitution =
        UserInstance.create(randomString(), scenarioContext.publication().getPublisher().getId());
    persistFile(uploaderWithoutInstitution, randomPendingOpenFile());
  }

  @Given("institution {string} has {int} pending file(s) without an approval ticket")
  public void institutionHasPendingFilesWithoutAnApprovalTicket(String institution, int fileCount) {
    persistPendingFiles(institution, fileCount);
  }

  @Given("institution {string} has {int} approved file(s)")
  public void institutionHasApprovedFiles(String institution, int fileCount) {
    persistFiles(institution, fileCount, AssociatedArtifactsGenerator::randomOpenFile);
  }

  @Given("institution {string} has {int} rejected file(s)")
  public void institutionHasRejectedFiles(String institution, int fileCount) {
    persistFiles(institution, fileCount, AssociatedArtifactsGenerator::randomRejectedFile);
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
    var ticket = scenarioContext.originalTicket(institution);
    ticket.setAssignee(new Username(randomString()));
    scenarioContext.ticketService().updateTicket(ticket);
  }

  @Given("a file from institution {string} is removed while the publication is unpublished")
  public void aFileFromInstitutionIsRemovedWhileThePublicationIsUnpublished(String institution) {
    fileEntryOf(scenarioContext.filesOf(institution).getFirst())
        .softDelete(scenarioContext.resourceService(), new User(randomString()));
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
    var customerId = scenarioContext.publication().getPublisher().getId();
    var autoPublishingCustomer =
        customerWithWorkflow(customerId, REGISTRATOR_PUBLISHES_METADATA_AND_FILES);
    scenarioContext.identityServiceClient().withCustomer(autoPublishingCustomer);
  }

  @When("the publication is republished")
  public void thePublicationIsRepublished() throws ApiGatewayException {
    new RepublishingService(
            scenarioContext.resourceService(), scenarioContext.identityServiceClient())
        .republish(scenarioContext.currentResource(), scenarioContext.editorAtOwnerInstitution());
  }

  @Then("institution {string} has a pending file approval ticket covering {int} file(s)")
  public void institutionHasAPendingFileApprovalTicketCoveringFiles(
      String institution, int fileCount) {
    var tickets = pendingTicketsOwnedBy(scenarioContext.institutionUri(institution));

    assertThat(tickets).hasSize(1);
    assertThat(tickets.getFirst().getFilesForApproval()).hasSize(fileCount);
  }

  @Then("institution {string} has {int} pending file approval tickets")
  public void institutionHasPendingFileApprovalTickets(String institution, int ticketCount) {
    assertThat(pendingTicketsOwnedBy(scenarioContext.institutionUri(institution)))
        .hasSize(ticketCount);
  }

  @Then(
      "the publication owner's institution has a pending file approval ticket covering {int}"
          + " file(s)")
  public void thePublicationOwnersInstitutionHasAPendingFileApprovalTicketCoveringFiles(
      int fileCount) {
    var ownerInstitution = scenarioContext.publication().getResourceOwner().getOwnerAffiliation();
    var tickets = pendingTicketsOwnedBy(ownerInstitution);

    assertThat(tickets).hasSize(1);
    assertThat(tickets.getFirst().getFilesForApproval()).hasSize(fileCount);
    assertThat(tickets.getFirst().getReceivingOrganizationDetails().topLevelOrganizationId())
        .isEqualTo(ownerInstitution);
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

  @Then("the pending file approval ticket of institution {string} has no curator")
  public void thePendingFileApprovalTicketOfInstitutionHasNoCurator(String institution) {
    var tickets = pendingTicketsOwnedBy(scenarioContext.institutionUri(institution));

    assertThat(tickets).hasSize(1);
    assertThat(tickets.getFirst().getAssignee()).isNull();
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
    return persistFiles(
        institution, fileCount, AssociatedArtifactsGenerator::randomPendingOpenFile);
  }

  private List<File> persistFiles(String institution, int fileCount, Supplier<File> fileSupplier) {
    var uploader = scenarioContext.uploaderAt(institution);
    var files =
        range(0, fileCount).mapToObj(ignored -> persistFile(uploader, fileSupplier.get())).toList();
    scenarioContext.addFiles(institution, files);
    return files;
  }

  private File persistFile(UserInstance uploader, File file) {
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
