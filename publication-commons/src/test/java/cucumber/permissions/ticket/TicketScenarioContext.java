package cucumber.permissions.ticket;


import static cucumber.permissions.publication.PublicationScenarioContext.CURATING_INSTITUTION;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import cucumber.permissions.publication.PublicationScenarioContext;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.ReceivingOrganizationDetails;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.permissions.ticket.TicketPermissions;

public class TicketScenarioContext {

    private URI ticketReceiver = CURATING_INSTITUTION;
    private final PublicationScenarioContext publicationScenarioContext;

    private TicketOperation operation;

    public TicketScenarioContext(PublicationScenarioContext publicationScenarioContext) {
        this.publicationScenarioContext = publicationScenarioContext;
    }

    public void setTicketReceiver(URI ticketReceiver) {
        this.ticketReceiver = ticketReceiver;
    }

    public TicketOperation getOperation() {
        return operation;
    }

    public void setOperation(TicketOperation operation) {
        this.operation = operation;
    }


    public TicketPermissions getTicketPermissions() {
        var resource = publicationScenarioContext.createResource();
        var userInstance = publicationScenarioContext.getUserInstance();
        var ticket = getTicketEntry(resource);

        return new TicketPermissions(ticket, userInstance, resource, new PublicationPermissions(resource, userInstance));
    }

    private TicketEntry getTicketEntry(Resource resource) {
        var ticket = attempt(() -> TicketEntry.createNewTicket(resource.toPublication(), PublishingRequestCase.class,
                                                               SortableIdentifier::next)).orElseThrow();
        ticket.setReceivingOrganizationDetails(new ReceivingOrganizationDetails(ticketReceiver, randomUri()));
        return ticket;
    }


}
