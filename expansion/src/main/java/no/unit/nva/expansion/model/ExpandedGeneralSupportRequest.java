package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.*;

import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

import static java.util.Objects.isNull;

@JsonTypeName(ExpandedGeneralSupportRequest.TYPE)
public class ExpandedGeneralSupportRequest extends ExpandedTicket {

    public static final String TYPE = "GeneralSupportCase";

    private Instant modifiedDate;
    private Set<URI> organizationIds;
    private Instant createdDate;
    private URI customerId;
    private ExpandedTicketStatus status;
    private User owner;

    public static ExpandedDataEntry createEntry(GeneralSupportRequest dataEntry, ResourceService resourceService,
                                                ResourceExpansionService resourceExpansionService,
                                                TicketService ticketService) throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(dataEntry.extractPublicationIdentifier());
        var entry = new ExpandedGeneralSupportRequest();
        var publicationSummary = PublicationSummary.create(publication);
        entry.setPublication(publicationSummary);
        entry.setOrganizationIds(resourceExpansionService.getOrganizationIds(dataEntry));
        entry.setStatus(getExpandedTicketStatus(dataEntry));
        entry.setOwner(dataEntry.getOwner());
        entry.setModifiedDate(dataEntry.getModifiedDate());
        entry.setCreatedDate(dataEntry.getCreatedDate());
        entry.setCustomerId(dataEntry.getCustomerId());
        entry.setId(generateId(publicationSummary.getPublicationId(), dataEntry.getIdentifier()));
        entry.setMessages(dataEntry.fetchMessages(ticketService));
        entry.setViewedBy(dataEntry.getViewedBy());
        entry.setFinalizedBy(dataEntry.getFinalizedBy());
        entry.setOwner(dataEntry.getOwner());
        return entry;
    }

    private static ExpandedTicketStatus getExpandedTicketStatus(GeneralSupportRequest generalSupportRequest) {
        switch (generalSupportRequest.getStatus()) {
            case PENDING:
                return getNewTicketStatus(generalSupportRequest);
            case COMPLETED:
                return ExpandedTicketStatus.COMPLETED;
            case CLOSED:
                return ExpandedTicketStatus.CLOSED;
        }
        return null;
    }

    private static ExpandedTicketStatus getNewTicketStatus(GeneralSupportRequest generalSupportRequest) {
        if (isNull(generalSupportRequest.getAssignee())) {
            return ExpandedTicketStatus.NEW;
        } else {
            return ExpandedTicketStatus.PENDING;
        }
    }

    private TicketStatus getTicketStatusParse() {
        if (!this.getStatus().equals(ExpandedTicketStatus.NEW)) {
            return TicketStatus.parse(this.getStatus().toString());
        }
        return null;
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
        ticketEntry.setStatus(getTicketStatusParse());
        ticketEntry.setOwner(this.getOwner());
        return ticketEntry;
    }

    @Override
    public ExpandedTicketStatus getStatus() {
        return this.status;
    }

    public void setStatus(ExpandedTicketStatus status) {
        this.status = status;
    }

    public User getOwner() {
        return this.owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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
