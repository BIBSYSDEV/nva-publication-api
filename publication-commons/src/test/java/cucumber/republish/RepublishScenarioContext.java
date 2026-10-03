package cucumber.republish;

import static java.util.Collections.emptyList;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import no.unit.nva.stubs.FakeIdentityServiceClient;
import nva.commons.apigateway.AccessRight;

/**
 * Holds the persistence layer and the publication under test for the republishing scenarios. Per
 * institution name used in a feature file, it keeps a stable institution URI, the files uploaded
 * from that institution, and the approval ticket it had before republishing.
 */
public class RepublishScenarioContext extends ResourcesLocalTest {

  private final Map<String, URI> institutions = new HashMap<>();
  private final Map<String, List<File>> filesByInstitution = new HashMap<>();
  private final Map<String, SortableIdentifier> originalTicketByInstitution = new HashMap<>();
  private final FakeIdentityServiceClient identityServiceClient =
      new FakeIdentityServiceClient().withDefaultCustomers();
  private ResourceService resourceService;
  private TicketService ticketService;
  private Publication publication;

  public void startScenario() {
    init();
    resourceService = getResourceService(client);
    ticketService = getTicketService();
  }

  public void endScenario() {
    shutdown();
  }

  public ResourceService resourceService() {
    return resourceService;
  }

  public TicketService ticketService() {
    return ticketService;
  }

  public FakeIdentityServiceClient identityServiceClient() {
    return identityServiceClient;
  }

  public Publication publication() {
    return publication;
  }

  public void setPublication(Publication publication) {
    this.publication = publication;
  }

  public Resource currentResource() {
    return Resource.resourceQueryObject(publication.getIdentifier())
        .fetch(resourceService)
        .orElseThrow();
  }

  public UserInstance publicationOwner() {
    return UserInstance.fromPublication(publication);
  }

  public UserInstance editorAtOwnerInstitution() {
    return UserInstance.create(
        randomString(),
        publication.getPublisher().getId(),
        randomUri(),
        List.of(AccessRight.MANAGE_RESOURCES_ALL),
        publication.getResourceOwner().getOwnerAffiliation());
  }

  public UserInstance uploaderAt(String institution) {
    var owner = new ResourceOwner(new Username(randomString()), institutionUri(institution));
    return UserInstance.create(owner, publication.getPublisher().getId());
  }

  public URI institutionUri(String institution) {
    return institutions.computeIfAbsent(institution, ignored -> randomUri());
  }

  public List<FilesApprovalEntry> fileApprovalTickets() {
    return resourceService
        .fetchAllTicketsForResource(Resource.fromPublication(publication))
        .filter(FilesApprovalEntry.class::isInstance)
        .map(FilesApprovalEntry.class::cast)
        .toList();
  }

  public List<FilesApprovalEntry> pendingFileApprovalTickets() {
    return fileApprovalTickets().stream().filter(TicketEntry::isPending).toList();
  }

  public void addFiles(String institution, List<File> files) {
    filesByInstitution.computeIfAbsent(institution, ignored -> new ArrayList<>()).addAll(files);
  }

  public List<File> filesOf(String institution) {
    return filesByInstitution.getOrDefault(institution, emptyList());
  }

  public void setOriginalTicket(String institution, TicketEntry ticket) {
    originalTicketByInstitution.put(institution, ticket.getIdentifier());
  }

  public TicketEntry originalTicket(String institution) {
    var identifier = originalTicketByInstitution.get(institution);
    return attempt(() -> ticketService.fetchTicketByIdentifier(identifier)).orElseThrow();
  }
}
