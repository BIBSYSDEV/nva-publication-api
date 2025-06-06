package no.unit.nva.expansion.model;

import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.utils.ExpandedTicketStatusMapper;
import no.unit.nva.expansion.utils.ExpansionUtil;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

public class ExpandedPublishingRequest extends ExpandedTicket {

    public static final String TYPE = "PublishingRequest";

    private PublishingWorkflow workflow;
    private Set<File> approvedFiles;
    private Set<File> filesForApproval;

    public ExpandedPublishingRequest() {
        super();
    }

    public static ExpandedPublishingRequest createEntry(PublishingRequestCase publishingRequestCase,
                                                        ResourceService resourceService,
                                                        ResourceExpansionService resourceExpansionService,
                                                        TicketService ticketService)
        throws NotFoundException {

        var publication = fetchPublication(publishingRequestCase, resourceService);
        var organization = resourceExpansionService.getOrganization(publishingRequestCase);
        var messages = expandMessages(publishingRequestCase.fetchMessages(ticketService), resourceExpansionService);
        var workflow = publishingRequestCase.getWorkflow();
        var owner = resourceExpansionService.expandPerson(publishingRequestCase.getOwner());
        var assignee = ExpansionUtil.expandPerson(publishingRequestCase.getAssignee(), resourceExpansionService);
        var finalizedBy = ExpansionUtil.expandPerson(publishingRequestCase.getFinalizedBy(), resourceExpansionService);
        var viewedBy = ExpansionUtil.expandPersonViewedBy(publishingRequestCase.getViewedBy(),
                                                          resourceExpansionService);
        return createRequest(publishingRequestCase, publication, organization, messages, workflow, owner, assignee,
                             finalizedBy, viewedBy);
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

    public Set<File> getFilesForApproval() {
        return filesForApproval;
    }

    public void setFilesForApproval(Set<File> filesForApproval) {
        this.filesForApproval = filesForApproval;
    }

    public Set<File> getApprovedFiles() {
        return approvedFiles;
    }

    public void setApprovedFiles(Set<File> approvedFiles) {
        this.approvedFiles = approvedFiles;
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
    }

    private static List<ExpandedMessage> expandMessages(List<Message> messages,
                                                        ResourceExpansionService expansionService) {
        return messages.stream().map(expansionService::expandMessage).collect(Collectors.toList());
    }

    private static ExpandedPublishingRequest createRequest(PublishingRequestCase dataEntry,
                                                           Publication publication,
                                                           ExpandedOrganization organization,
                                                           List<ExpandedMessage> messages,
                                                           PublishingWorkflow workflow,
                                                           ExpandedPerson owner,
                                                           ExpandedPerson assignee,
                                                           ExpandedPerson finalizedBy,
                                                           Set<ExpandedPerson> viewedBy) {
        var publicationSummary = PublicationSummary.create(publication);
        var entry = new ExpandedPublishingRequest();
        entry.setId(generateId(publicationSummary.getPublicationId(), dataEntry.getIdentifier()));
        entry.setPublication(publicationSummary);
        entry.setOrganization(organization);
        entry.setStatus(ExpandedTicketStatusMapper.getExpandedTicketStatus(dataEntry));
        entry.setCustomerId(dataEntry.getCustomerId());
        entry.setCreatedDate(dataEntry.getCreatedDate());
        entry.setModifiedDate(dataEntry.getModifiedDate());
        entry.setMessages(messages);
        entry.setViewedBy(viewedBy);
        entry.setWorkflow(workflow);
        entry.setFinalizedBy(finalizedBy);
        entry.setOwner(owner);
        entry.setAssignee(assignee);
        entry.setApprovedFiles(dataEntry.getApprovedFiles());
        entry.setFilesForApproval(dataEntry.getFilesForApproval());
        return entry;
    }

    private static Publication fetchPublication(PublishingRequestCase publishingRequestCase,
                                                ResourceService resourceService) {
        return attempt(() -> resourceService.getPublicationByIdentifier(
            publishingRequestCase.getResourceIdentifier())).orElseThrow();
    }
}