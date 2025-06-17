package cucumber.permissions.ticket;

import static cucumber.permissions.enums.TicketStatusConfig.COMPLETED;
import static cucumber.permissions.enums.TicketStatusConfig.PENDING;
import static cucumber.permissions.publication.PublicationScenarioContext.CURATING_INSTITUTION;
import static cucumber.permissions.publication.PublicationScenarioContext.NON_CURATING_INSTITUTION;
import static java.util.Objects.nonNull;
import static no.unit.nva.model.testing.PublicationGenerator.randomUri;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import cucumber.permissions.enums.PublicationTypeConfig;
import cucumber.permissions.enums.TicketOwnerConfig;
import cucumber.permissions.enums.TicketStatusConfig;
import cucumber.permissions.enums.TicketTypeConfig;
import cucumber.permissions.publication.PublicationScenarioContext;
import java.net.URI;
import java.util.Collections;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.permissions.publication.PublicationPermissions;
import no.unit.nva.publication.permissions.ticket.TicketPermissions;

public class TicketScenarioContext {

    private URI ticketReceiver = CURATING_INSTITUTION;
    private final PublicationScenarioContext publicationScenarioContext;

    private TicketOperation operation;
    private TicketOwnerConfig ticketOwner = TicketOwnerConfig.USER;
    private TicketTypeConfig ticketType = TicketTypeConfig.FILE_APPROVAL;
    private TicketStatusConfig ticketStatus = PENDING;

    public TicketScenarioContext(PublicationScenarioContext publicationScenarioContext) {
        this.publicationScenarioContext = publicationScenarioContext;
    }

    public void setTicketReceiver(URI ticketReceiver) {
        this.ticketReceiver = ticketReceiver;
    }

    public void setTicketCreator(TicketOwnerConfig ticketOwner) {
        this.ticketOwner = ticketOwner;
    }

    public TicketOwnerConfig getTicketOwner() {
        return ticketOwner;
    }
    
    public void setTicketType(TicketTypeConfig ticketTypeConfig) {
        this.ticketType = ticketTypeConfig;
    }

    public TicketTypeConfig getTicketType() {
        return ticketType;
    }

    public TicketOperation getOperation() {
        return operation;
    }

    public void setTicketStatus(TicketStatusConfig ticketStatusConfig) {
        this.ticketStatus = ticketStatusConfig;
    }

    public TicketStatusConfig getTicketStatus() {
        return ticketStatus;
    }

    public void setOperation(TicketOperation operation) {
        this.operation = operation;
    }

    public TicketPermissions getTicketPermissions() {
        var resource = publicationScenarioContext.createResource();
        var userInstance = publicationScenarioContext.getUserInstance();
        var ticket = getTicketEntry(resource, userInstance);

        return new TicketPermissions(ticket, userInstance, resource,
                                     new PublicationPermissions(resource, userInstance));
    }

    private TicketEntry getTicketEntry(Resource resource, UserInstance userInstance) {

        var ticketCreator = switch (getTicketOwner()) {
            case USER -> nonNull(userInstance) ? userInstance : UserInstance.create(randomString(), randomUri());
            case OTHER_USER_AT_SAME_INSTITUTION -> UserInstance.create(randomString(), randomUri(), randomUri(),
                                                                       Collections.emptyList(),
                                                                       userInstance.getTopLevelOrgCristinId());
            case OTHER_USER_AT_DIFFERENT_INSTITUTION -> UserInstance.create(randomString(), randomUri());
        };

        var ticket = switch (getTicketType()) {
            case FILE_APPROVAL -> createFileApprovalTicket(resource, ticketCreator);
            case DOI_REQUEST -> createDoiRequestTicket(resource, ticketCreator);
            case SUPPORT_REQUEST -> createSupportRequestTicket(resource, ticketCreator);
            default -> throw new IllegalArgumentException("Unsupported ticket type: " + getTicketType());
        };

        ticket.setStatus(COMPLETED.equals(getTicketStatus()) ? TicketStatus.COMPLETED : TicketStatus.PENDING);
        return ticket;
    }

    private TicketEntry createSupportRequestTicket(Resource resource, UserInstance userInstance) {
        return GeneralSupportRequest.create(resource, userInstance);
    }

    private TicketEntry createDoiRequestTicket(Resource resource, UserInstance userInstance) {
        return DoiRequest.create(resource, userInstance);
    }

    private TicketEntry createFileApprovalTicket(Resource resource, UserInstance ticketCreator) {
        if (PublicationTypeConfig.DEGREE.equals(publicationScenarioContext.getPublicationTypeConfig())) {
            return switch (publicationScenarioContext.getChannelClaimConfig()) {
                case NON_CLAIMED -> FilesApprovalThesis.createForUserInstitution(resource, ticketCreator, null);
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
