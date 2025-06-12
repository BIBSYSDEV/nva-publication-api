package cucumber.permissions.ticket;

import static cucumber.permissions.publication.PublicationScenarioContext.CURATING_INSTITUTION;
import static cucumber.permissions.publication.PublicationScenarioContext.NON_CURATING_INSTITUTION;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import cucumber.permissions.enums.PublicationTypeConfig;
import cucumber.permissions.publication.PublicationScenarioContext;
import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UserInstance;
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
        var ticket = getTicketEntry(resource, userInstance);

        return new TicketPermissions(ticket, userInstance, resource, new PublicationPermissions(resource, userInstance));
    }

    private TicketEntry getTicketEntry(Resource resource, UserInstance userInstance) {
        var ticketCreator = nonNull(userInstance) ? userInstance : UserInstance.create(randomString(), randomUri());

        if (PublicationTypeConfig.DEGREE.equals(publicationScenarioContext.getPublicationTypeConfig())) {
            return switch(publicationScenarioContext.getChannelClaimConfig()) {
                case NON_CLAIMED -> FilesApprovalThesis.createForUserInstitution(resource, userInstance, null);
                case CLAIMED_BY_USERS_INSTITUTION -> FilesApprovalThesis.createForChannelOwningInstitution(
                    resource,
                    ticketCreator,
                    ticketCreator.getTopLevelOrgCristinId(),
                    SortableIdentifier.next(),
                    null);
                case CLAIMED_BY_NOT_USERS_INSTITUTION -> FilesApprovalThesis.createForChannelOwningInstitution(
                    resource,
                    ticketCreator,
                    NON_CURATING_INSTITUTION.equals(ticketCreator.getTopLevelOrgCristinId())
                        ? CURATING_INSTITUTION
                        : NON_CURATING_INSTITUTION,
                    SortableIdentifier.next(),
                    null);
            };
        }

        return PublishingRequestCase.create(resource, ticketCreator, null);
    }
}
