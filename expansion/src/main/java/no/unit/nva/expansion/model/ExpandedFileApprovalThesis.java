package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.utils.ExpandedTicketStatusMapper;
import no.unit.nva.expansion.utils.ExpansionUtil;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;

@JsonTypeName(ExpandedFileApprovalThesis.TYPE)
public class ExpandedFileApprovalThesis extends ExpandedTicket {

    public static final String TYPE = "FileApprovalThesis";


    private PublishingWorkflow workflow;
    private Set<File> approvedFiles;
    private Set<File> filesForApproval;

    public static ExpandedDataEntry createEntry(FilesApprovalThesis dataEntry, ResourceService resourceService,
                                                ResourceExpansionService resourceExpansionService,
                                                TicketService ticketService) throws NotFoundException {
        var publication = resourceService.getPublicationByIdentifier(dataEntry.getResourceIdentifier());
        var entry = new ExpandedFileApprovalThesis();
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
        entry.setApprovedFiles(dataEntry.getApprovedFiles());
        entry.setFilesForApproval(dataEntry.getFilesForApproval());
        entry.setWorkflow(dataEntry.getWorkflow());
        return entry;
    }

    public Set<File> getFilesForApproval() {
        return filesForApproval;
    }

    private void setFilesForApproval(Set<File> filesForApproval) {
        this.filesForApproval = filesForApproval;
    }

    public Set<File> getApprovedFiles() {
        return approvedFiles;
    }

    private void setApprovedFiles(Set<File> approvedFiles) {
        this.approvedFiles = approvedFiles;
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    private void setWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return new SortableIdentifier(UriWrapper.fromUri(getId()).getLastPathElement());
    }

    private static List<ExpandedMessage> expandMessages(List<Message> messages,
                                                        ResourceExpansionService expansionService) {
        return messages.stream()
                   .map(expansionService::expandMessage)
                   .collect(Collectors.toList());
    }
}
