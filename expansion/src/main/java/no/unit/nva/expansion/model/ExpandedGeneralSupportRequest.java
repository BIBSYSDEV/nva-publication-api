package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@JsonTypeName(ExpandedGeneralSupportRequest.TYPE)
public class ExpandedGeneralSupportRequest extends ExpandedTicket {

    public static final String TYPE = "GeneralSupportCase";

    private Instant modifiedDate;
    private Set<URI> organizationIds;
    private Instant createdDate;
    private URI customerId;
    private TicketStatus status;

    public static ExpandedDataEntry createEntry(GeneralSupportRequest dataEntry, ResourceService resourceService,
                                                ResourceExpansionService resourceExpansionService,
                                                TicketService ticketService) throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(dataEntry.extractPublicationIdentifier());
        var entry = new ExpandedGeneralSupportRequest();
        var publicationSummary = PublicationSummary.create(publication);
        entry.setPublication(publicationSummary);
        entry.setOrganizationIds(resourceExpansionService.getOrganizationIds(dataEntry));
        entry.setStatus(dataEntry.getStatus());
        entry.setOwner(resourceExpansionService.expandPerson(dataEntry.getOwner()));
        entry.setModifiedDate(dataEntry.getModifiedDate());
        entry.setCreatedDate(dataEntry.getCreatedDate());
        entry.setCustomerId(dataEntry.getCustomerId());
        entry.setId(generateId(publicationSummary.getPublicationId(), dataEntry.getIdentifier()));
        entry.setMessages(dataEntry.fetchMessages(ticketService));
        entry.setViewedBy(dataEntry.getViewedBy());
        entry.setFinalizedBy(dataEntry.getFinalizedBy());
        entry.setAssignee(expandAssignee(dataEntry, resourceExpansionService));
        return entry;
    }

    private static ExpandedPerson expandAssignee(GeneralSupportRequest generalSupportRequest,
                                                 ResourceExpansionService expansionService) {
        return Optional.ofNullable(generalSupportRequest.getAssignee())
                .map(Username::getValue)
                .map(User::new)
                .map(expansionService::expandPerson)
                .orElse(null);
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return new SortableIdentifier(UriWrapper.fromUri(getId()).getLastPathElement());
    }

    @Override
    public Set<URI> getOrganizationIds() {
        return this.organizationIds;
    }

    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }

    @Override
    public GeneralSupportRequest toTicketEntry() {
        var ticketEntry = new GeneralSupportRequest();
        ticketEntry.setModifiedDate(this.getModifiedDate());
        ticketEntry.setCreatedDate(this.getCreatedDate());
        ticketEntry.setCustomerId(this.getCustomerId());
        ticketEntry.setIdentifier(extractIdentifier(this.getId()));
        ticketEntry.setPublicationDetails(PublicationDetails.create(this.getPublication()));
        ticketEntry.setStatus(this.getStatus());
        ticketEntry.setOwner(this.getOwner().getUsername());
        ticketEntry.setAssignee(extractAssigneeUsername());
        return ticketEntry;
    }

    private Username extractAssigneeUsername() {
        return Optional.ofNullable(this.getAssignee())
                .map(ExpandedPerson::getUsername)
                .map(User::toString)
                .map(Username::new)
                .orElse(null);
    }

    @Override
    public TicketStatus getStatus() {
        return this.status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    private URI getCustomerId() {
        return this.customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    private Instant getCreatedDate() {
        return this.createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
}
