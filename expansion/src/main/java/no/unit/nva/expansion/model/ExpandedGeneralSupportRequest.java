package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;

@JsonTypeName(ExpandedGeneralSupportRequest.TYPE)
public class ExpandedGeneralSupportRequest extends ExpandedTicket {
    
    public static final String TYPE = "GeneralSupportRequest";
    
    private Instant modifiedDate;
    private Set<URI> organizationIds;
    private Instant createdDate;
    private URI customerId;
    private TicketStatus status;
    private User owner;
    
    public static ExpandedDataEntry createEntry(GeneralSupportRequest dataEntry, ResourceService resourceService,
                                                ResourceExpansionService resourceExpansionService,
                                                TicketService ticketService) throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(dataEntry.extractPublicationIdentifier());
        var entry = new ExpandedGeneralSupportRequest();
        var publicationSummary = PublicationSummary.create(publication);
        entry.setPublication(publicationSummary);
        entry.setOrganizationIds(resourceExpansionService.getOrganizationIds(dataEntry));
        entry.setStatus(dataEntry.getStatus());
        entry.setOwner(dataEntry.getOwner());
        entry.setModifiedDate(dataEntry.getModifiedDate());
        entry.setCreatedDate(dataEntry.getCreatedDate());
        entry.setCustomerId(dataEntry.getCustomerId());
        entry.setId(generateId(publicationSummary.getPublicationId(), dataEntry.getIdentifier()));
        entry.setMessages(dataEntry.fetchMessages(ticketService));
        entry.setViewedBy(dataEntry.getViewedBy());
        return entry;
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
        ticketEntry.setOwner(this.getOwner());
        return ticketEntry;
    }
    
    @Override
    public TicketStatus getStatus() {
        return this.status;
    }
    
    public void setStatus(TicketStatus status) {
        this.status = status;
    }
    
    private User getOwner() {
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
