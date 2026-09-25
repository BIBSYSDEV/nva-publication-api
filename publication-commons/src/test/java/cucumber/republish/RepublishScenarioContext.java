package cucumber.republish;

import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.unit.nva.model.Publication;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.FakeCustomerApiClient;
import no.unit.nva.publication.service.ResourcesLocalTest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;

/**
 * Holds the persistence layer and the publication under test for the republishing scenarios, and
 * keeps one stable institution URI per institution name used in a feature file.
 */
public class RepublishScenarioContext extends ResourcesLocalTest {

  private final Map<String, URI> institutions = new HashMap<>();
  private final FakeCustomerApiClient customerApiClient = new FakeCustomerApiClient();
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

  public FakeCustomerApiClient customerApiClient() {
    return customerApiClient;
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
}
