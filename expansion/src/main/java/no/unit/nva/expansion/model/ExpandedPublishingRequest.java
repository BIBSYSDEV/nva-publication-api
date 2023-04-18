package no.unit.nva.expansion.model;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class ExpandedPublishingRequest extends ExpandedTicket {
    
    public static final String TYPE = "PublishingRequest";
    public static final String STATUS_FIELD = "status";
    
    @JsonProperty("organizationIds")
    private Set<URI> organizationIds;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    private URI customerId;
    private User owner;
    private Instant modifiedDate;
    private Instant createdDate;

    private PublishingWorkflow workflow;
    
    public ExpandedPublishingRequest() {
        super();
    }
    
    public static ExpandedPublishingRequest createEntry(PublishingRequestCase publishingRequestCase,
                                                        ResourceService resourceService,
                                                        ResourceExpansionService resourceExpansionService,
                                                        TicketService ticketService)
        throws NotFoundException {
        
        var publication = fetchPublication(publishingRequestCase, resourceService);
        var organizationIds = resourceExpansionService.getOrganizationIds(publishingRequestCase);
        var messages = publishingRequestCase.fetchMessages(ticketService);
        var workflow = publishingRequestCase.getWorkflow();
        return createRequest(publishingRequestCase, publication, organizationIds, messages, workflow);
    }
    
    @JacocoGenerated
    @Override
    public String toJsonString() {
        return super.toJsonString();
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return extractIdentifier(getId());
    }
    
    @Override
    public Set<URI> getOrganizationIds() {
        return nonNull(organizationIds) ? organizationIds : Collections.emptySet();
    }
    
    @Override
    public TicketEntry toTicketEntry() {
        var publishingRequest = new PublishingRequestCase();
        publishingRequest.setPublicationDetails(PublicationDetails.create(getPublication()));
        publishingRequest.setCustomerId(this.getCustomerId());
        publishingRequest.setIdentifier(extractIdentifier(this.getId()));
        publishingRequest.setOwner(this.getOwner());
        publishingRequest.setModifiedDate(this.getModifiedDate());
        publishingRequest.setCreatedDate(this.getCreatedDate());
        publishingRequest.setStatus(this.getStatus());
        return publishingRequest;
    }
    
    @Override
    public TicketStatus getStatus() {
        return status;
    }
    
    public void setStatus(TicketStatus status) {
        this.status = status;
    }
    
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }
    
    public Instant getCreatedDate() {
        return this.createdDate;
    }
    
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    public Instant getModifiedDate() {
        return this.modifiedDate;
    }
    
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    public User getOwner() {
        return this.owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    public URI getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
    }

    private static ExpandedPublishingRequest createRequest(PublishingRequestCase dataEntry,
                                                           Publication publication,
                                                           Set<URI> organizationIds,
                                                           List<Message> messages,
                                                           PublishingWorkflow workflow) {
        var publicationSummary = PublicationSummary.create(publication);
        var entry = new ExpandedPublishingRequest();
        entry.setId(generateId(publicationSummary.getPublicationId(), dataEntry.getIdentifier()));
        entry.setPublication(publicationSummary);
        entry.setOrganizationIds(organizationIds);
        entry.setStatus(dataEntry.getStatus());
        entry.setCustomerId(dataEntry.getCustomerId());
        entry.setCreatedDate(dataEntry.getCreatedDate());
        entry.setModifiedDate(dataEntry.getModifiedDate());
        entry.setOwner(dataEntry.getOwner());
        entry.setMessages(messages);
        entry.setViewedBy(dataEntry.getViewedBy());
        entry.setWorkflow(workflow);
        return entry;
    }
    
    private static Publication fetchPublication(PublishingRequestCase publishingRequestCase,
                                                ResourceService resourceService) {
        return attempt(() -> resourceService.getPublicationByIdentifier(
            publishingRequestCase.extractPublicationIdentifier())).orElseThrow();
    }
}
