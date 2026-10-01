package no.unit.nva.publication.model.business;

import static java.util.Collections.emptyList;
import static no.unit.nva.model.testing.PublicationGenerator.randomNonDegreePublication;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.model.testing.associatedartifacts.AssociatedArtifactsGenerator.randomPendingOpenFile;
import static no.unit.nva.publication.model.business.TicketStatus.NOT_APPLICABLE;
import static no.unit.nva.publication.model.business.TicketStatus.PENDING;
import static no.unit.nva.publication.service.FakeCustomerApiClient.METADATA_ONLY_CUSTOMER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.ResourceOwner;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.AssociatedArtifactList;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.FilesApprovalTickets;
import no.unit.nva.publication.service.FakeCustomerApiClient;
import org.junit.jupiter.api.Test;

class PendingFileTicketsTest {

  private final FakeCustomerApiClient customerApiClient = new FakeCustomerApiClient();

  @Test
  void shouldReuseSetAsideTicketThatIsIdenticalToTheTicketThatWouldBeCreated() {
    var resource = resourceWithPendingFile();
    var setAsideTicket = setAside(ticketFor(resource));

    var changes =
        PendingFileTickets.changesFor(resource, List.of(setAsideTicket), customerApiClient);

    assertThat(changes.changedTickets()).containsExactly(setAsideTicket);
    assertThat(changes.newTickets()).isEmpty();
    assertThat(setAsideTicket.getStatus()).isEqualTo(PENDING);
  }

  @Test
  void shouldReplaceSetAsideTicketThatIsRoutedByAChannelClaim() {
    var resource = resourceWithPendingFile();
    var setAsideTicket = setAside(ticketFor(resource));
    var sameInstitution = setAsideTicket.getReceivingOrganizationDetails().topLevelOrganizationId();
    setAsideTicket.applyPublicationChannelClaim(sameInstitution, SortableIdentifier.next());

    var changes =
        PendingFileTickets.changesFor(resource, List.of(setAsideTicket), customerApiClient);

    assertThat(changes.newTickets()).hasSize(1);
    assertThat(setAsideTicket.getStatus()).isEqualTo(NOT_APPLICABLE);
  }

  private static Resource resourceWithPendingFile() {
    var publication =
        randomNonDegreePublication()
            .copy()
            .withStatus(PublicationStatus.PUBLISHED)
            .withAssociatedArtifacts(new AssociatedArtifactList(emptyList()))
            .build();
    var resource = Resource.fromPublication(publication);
    var uploader =
        UserInstance.create(
            new ResourceOwner(new Username(randomString()), randomUri()),
            publication.getPublisher().getId());
    var fileEntry = FileEntry.create(randomPendingOpenFile(), resource.getIdentifier(), uploader);
    resource.setFileEntries(List.of(fileEntry));
    resource.setAssociatedArtifacts(new AssociatedArtifactList(List.of(fileEntry.getFile())));
    return resource;
  }

  private static FilesApprovalEntry ticketFor(Resource resource) {
    var fileEntry = resource.getFileEntries().getFirst();
    return new FilesApprovalTickets(UserInstance.fromFileEntry(fileEntry), METADATA_ONLY_CUSTOMER)
        .newTicket(resource, Set.of(fileEntry.getFile()));
  }

  private static FilesApprovalEntry setAside(FilesApprovalEntry ticket) {
    ticket.setStatus(NOT_APPLICABLE);
    return ticket;
  }
}
