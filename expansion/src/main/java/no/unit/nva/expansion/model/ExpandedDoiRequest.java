package no.unit.nva.expansion.model;

import static no.unit.nva.expansion.utils.ExpansionUtil.expandPerson;
import static no.unit.nva.expansion.utils.ExpansionUtil.expandPersonViewedBy;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.WithOrganizationScope;
import no.unit.nva.expansion.utils.ExpandedTicketStatusMapper;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(ExpandedDoiRequest.TYPE)
@SuppressWarnings("PMD.TooManyFields")
public final class ExpandedDoiRequest extends ExpandedTicket implements WithOrganizationScope {

    public static final String TYPE = "DoiRequest";

    @JsonProperty()
    private ExpandedTicketStatus status;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    @JsonAlias("date")
    private Instant createdDate;
    @JsonProperty
    private URI customerId;
    @JsonProperty("doi")
    private URI doi;
    @JsonProperty(ORGANIZATION_IDS_FIELD)
    private Set<URI> organizationIds;

    public static ExpandedDoiRequest createEntry(DoiRequest doiRequest,
                                                 ResourceExpansionService expansionService,
                                                 ResourceService resourceService,
                                                 TicketService ticketService)
        throws NotFoundException {
        var expandedDoiRequest = ExpandedDoiRequest.fromDoiRequest(doiRequest, resourceService, expansionService);
        expandedDoiRequest.setOrganizationIds(fetchOrganizationIdsForViewingScope(doiRequest, expansionService));
        expandedDoiRequest.setMessages(expandMessages(doiRequest.fetchMessages(ticketService), expansionService));
        expandedDoiRequest.setOwner(expansionService.expandPerson(doiRequest.getOwner()));
        expandedDoiRequest.setAssignee(expandPerson(doiRequest.getAssignee(), expansionService));
        expandedDoiRequest.setFinalizedBy(expandPerson(doiRequest.getFinalizedBy(), expansionService));
        expandedDoiRequest.setViewedBy(expandPersonViewedBy(doiRequest.getViewedBy(), expansionService));
        return expandedDoiRequest;
    }

    @JacocoGenerated
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @JacocoGenerated
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @JacocoGenerated
    public Instant getCreatedDate() {
        return createdDate;
    }

    @JacocoGenerated
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }

    @JacocoGenerated
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @JacocoGenerated
    public URI getDoi() {
        return doi;
    }

    @JacocoGenerated
    public void setDoi(URI doi) {
        this.doi = doi;
    }

    @JacocoGenerated
    @Override
    public Set<URI> getOrganizationIds() {
        return organizationIds;
    }

    @JacocoGenerated
    @Override
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }

    @Override
    @JacocoGenerated
    public ExpandedTicketStatus getStatus() {
        return status;
    }

    @JacocoGenerated
    public void setStatus(ExpandedTicketStatus status) {
        this.status = status;
    }

    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return extractIdentifier(getId());
    }

    private static List<ExpandedMessage> expandMessages(List<Message> messages,
                                                        ResourceExpansionService expansionService) {
        return messages.stream()
            .map(expansionService::expandMessage)
            .collect(Collectors.toList());
    }

    private static Set<URI> fetchOrganizationIdsForViewingScope(DoiRequest doiRequest,
                                                                ResourceExpansionService resourceExpansionService)
        throws NotFoundException {
        return resourceExpansionService.getOrganizationIds(doiRequest);
    }

    // should not become public. An ExpandedDoiRequest needs an Expansion service to be complete
    private static ExpandedDoiRequest fromDoiRequest(DoiRequest doiRequest, ResourceService resourceService,
                                                     ResourceExpansionService resourceExpansionService) {
        var publicationSummary = PublicationSummary.create(doiRequest.toPublication(resourceService));
        ExpandedDoiRequest entry = new ExpandedDoiRequest();
        entry.setPublication(publicationSummary);
        entry.setCreatedDate(doiRequest.getCreatedDate());
        entry.setId(generateId(publicationSummary.getPublicationId(), doiRequest.getIdentifier()));
        entry.setCustomerId(doiRequest.getCustomerId());
        entry.setModifiedDate(doiRequest.getModifiedDate());
        entry.setStatus(ExpandedTicketStatusMapper.getExpandedTicketStatus(doiRequest));
        entry.setViewedBy(expandPersonViewedBy(doiRequest.getViewedBy(), resourceExpansionService));
        entry.setPublication(publicationSummary);
        entry.setFinalizedBy(expandPerson(doiRequest.getFinalizedBy(), resourceExpansionService));
        return entry;
    }
}