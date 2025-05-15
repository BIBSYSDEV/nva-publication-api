package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.utils.ExpandedTicketStatusMapper;
import no.unit.nva.expansion.utils.ExpansionUtil;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.UnpublishRequest;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(ExpandedUnpublishRequest.TYPE)
public class ExpandedUnpublishRequest extends ExpandedTicket {

    public static final String TYPE = "UnpublishRequest";

    public static ExpandedDataEntry createEntry(UnpublishRequest dataEntry, ResourceService resourceService,
                                                ResourceExpansionService resourceExpansionService,
                                                TicketService ticketService) throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(dataEntry.getResourceIdentifier());
        var entry = new ExpandedUnpublishRequest();
        var publicationSummary = PublicationSummary.create(publication);
        entry.setPublication(publicationSummary);
        entry.setOrganization(resourceExpansionService.getOrganization(dataEntry));
        entry.setStatus(ExpandedTicketStatusMapper.getExpandedTicketStatus(dataEntry));
        entry.setOwner(resourceExpansionService.expandPerson(dataEntry.getOwner()));
        entry.setModifiedDate(dataEntry.getModifiedDate());
        entry.setCreatedDate(dataEntry.getCreatedDate());
        entry.setCustomerId(dataEntry.getCustomerId());
        entry.setId(generateId(publicationSummary.getPublicationId(), dataEntry.getIdentifier()));
        entry.setMessages(expandMessages(dataEntry.fetchMessages(ticketService), resourceExpansionService));
        entry.setViewedBy(ExpansionUtil.expandPersonViewedBy(dataEntry.getViewedBy(), resourceExpansionService));
        entry.setFinalizedBy(ExpansionUtil.expandPerson(dataEntry.getFinalizedBy(), resourceExpansionService));
        entry.setAssignee(ExpansionUtil.expandPerson(dataEntry.getAssignee(), resourceExpansionService));
        return entry;
    }

    @JacocoGenerated
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return extractIdentifier(getId());
    }

    @JacocoGenerated
    @Override
    public String toJsonString() {
        return super.toJsonString();
    }

    private static List<ExpandedMessage> expandMessages(List<Message> messages,
                                                        ResourceExpansionService expansionService) {
        return messages.stream()
                   .map(expansionService::expandMessage)
                   .collect(Collectors.toList());
    }
}
