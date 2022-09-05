package no.unit.nva.expansion.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.MessageCollection;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;

public class ExpandedGeneralSupportRequest implements ExpandedTicket {
    
    private SortableIdentifier identifier;
    private PublicationSummary publicationSummary;
    private Set<URI> organizationIds;
    private Instant createdDate;
    private Instant modifiedDate;
    private List<MessageCollection> messageCollection;
    
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }
    
    public void setPublicationSummary(PublicationSummary publicationSummary) {
        this.publicationSummary = publicationSummary;
    }
    
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }
    
    public static ExpandedDataEntry create(GeneralSupportRequest dataEntry,
                                           ResourceExpansionService expansionService,
                                           ResourceService resourceService,
                                           TicketService ticketService) throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(dataEntry.getResourceIdentifier());
        var expandedDataEntry = new ExpandedGeneralSupportRequest();
        expandedDataEntry.setIdentifier(dataEntry.getIdentifier());
        expandedDataEntry.createdDate = dataEntry.getCreatedDate();
        expandedDataEntry.modifiedDate = dataEntry.getModifiedDate();
        expandedDataEntry.organizationIds = expansionService.getOrganizationIds(dataEntry);
        expandedDataEntry.publicationSummary = PublicationSummary.create(publication);
        expandedDataEntry.messageCollection =
            MessageCollection.groupMessagesByType(dataEntry.fetchMessages(ticketService));
        return expandedDataEntry;
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return this.identifier;
    }
    
    @Override
    public PublicationSummary getPublicationSummary() {
        return this.publicationSummary;
    }
    
    @Override
    public Set<URI> getOrganizationIds() {
        return this.organizationIds;
    }
    
    @Override
    public Instant getCreatedDate() {
        return this.createdDate;
    }
    
    @Override
    public Instant getModifiedDate() {
        return this.modifiedDate;
    }
}
