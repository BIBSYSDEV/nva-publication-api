package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.utils.ExpandedTicketStatusMapper;
import no.unit.nva.expansion.utils.ExpansionUtil;
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
public final class ExpandedDoiRequest extends ExpandedTicket {

    public static final String TYPE = "DoiRequest";

    @JsonProperty("doi")
    private URI doi;

    public static ExpandedDoiRequest createEntry(DoiRequest doiRequest,
                                                 ResourceExpansionService expansionService,
                                                 ResourceService resourceService,
                                                 TicketService ticketService)
        throws NotFoundException {
        var expandedDoiRequest = ExpandedDoiRequest.fromDoiRequest(doiRequest, resourceService, expansionService);
        expandedDoiRequest.setOrganization(expansionService.getOrganization(doiRequest));
        expandedDoiRequest.setMessages(expandMessages(doiRequest.fetchMessages(ticketService), expansionService));
        expandedDoiRequest.setOwner(expansionService.expandPerson(doiRequest.getOwner()));
        expandedDoiRequest.setAssignee(ExpansionUtil.expandPerson(doiRequest.getAssignee(), expansionService));
        expandedDoiRequest.setFinalizedBy(ExpansionUtil.expandPerson(doiRequest.getFinalizedBy(), expansionService));
        expandedDoiRequest.setViewedBy(ExpansionUtil.expandPersonViewedBy(doiRequest.getViewedBy(), expansionService));
        return expandedDoiRequest;
    }

    @JacocoGenerated
    public URI getDoi() {
        return doi;
    }

    @JacocoGenerated
    public void setDoi(URI doi) {
        this.doi = doi;
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
        entry.setViewedBy(ExpansionUtil.expandPersonViewedBy(doiRequest.getViewedBy(), resourceExpansionService));
        entry.setPublication(publicationSummary);
        entry.setFinalizedBy(ExpansionUtil.expandPerson(doiRequest.getFinalizedBy(), resourceExpansionService));
        return entry;
    }
}