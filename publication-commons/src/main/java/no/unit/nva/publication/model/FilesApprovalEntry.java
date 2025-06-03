package no.unit.nva.publication.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.APPROVED_FILES_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.FILES_FOR_APPROVAL_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.WORKFLOW;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.publication.model.business.FileEntry;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.ReceivingOrganizationDetails;
import no.unit.nva.publication.model.business.Resource;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = FilesApprovalThesis.TYPE, value = FilesApprovalThesis.class),
    @JsonSubTypes.Type(name = PublishingRequestCase.TYPE, value = PublishingRequestCase.class)})
public abstract class FilesApprovalEntry extends TicketEntry {

    @JsonProperty(WORKFLOW)
    private PublishingWorkflow workflow;
    @JsonProperty(APPROVED_FILES_FIELD)
    private Set<File> approvedFiles;
    @JsonProperty(FILES_FOR_APPROVAL_FIELD)
    private Set<File> filesForApproval;

    @Override
    public abstract void validateCreationRequirements(Publication publication) throws ConflictException;

    @Override
    public FilesApprovalEntry complete(Publication publication, UserInstance userInstance) {
        var completed = (FilesApprovalEntry) super.complete(publication, userInstance);
        completed.emptyFilesForApproval();
        return completed;
    }

    public FilesApprovalEntry applyPublicationChannelClaim(URI organizationId,
                                                           SortableIdentifier channelClaimIdentifier) {
        this.setReceivingOrganizationDetails(new ReceivingOrganizationDetails(organizationId, organizationId, channelClaimIdentifier));
        return this;
    }

    public void undoPublicationChannelClaim() {
        this.setReceivingOrganizationDetails(
            new ReceivingOrganizationDetails(getOwnerAffiliation(), getResponsibilityArea()));
    }

    protected FilesApprovalEntry completeAndApproveFiles(Resource resource, UserInstance userInstance) {
        this.setAssignee(new Username(userInstance.getUsername()));
        this.approveFiles();
        return this.complete(resource.toPublication(), userInstance);
    }

    public TicketEntry persistAutoComplete(TicketService ticketService, Publication publication,
                                           UserInstance userInstance) throws ApiGatewayException {
        return this.complete(publication, userInstance).persistNewTicket(ticketService);
    }

    public void rejectRejectedFiles(ResourceService resourceService) {
        getFilesForApproval().stream()
            .map(PendingFile.class::cast)
            .forEach(file -> FileEntry.queryObject(file.getIdentifier(), getResourceIdentifier())
                                 .fetch(resourceService)
                                 .ifPresent(fileEntry -> fileEntry.reject(resourceService,
                                                                          new User(getFinalizedBy().getValue()))));
    }

    @Override
    public abstract TicketEntry copy();

    @Override
    public abstract TicketDao toDao();

    @Override
    public abstract String getType();

    public Set<File> getApprovedFiles() {
        return nonNull(approvedFiles) ? approvedFiles : Set.of();
    }

    public void setApprovedFiles(Set<File> approvedFiles) {
        this.approvedFiles = approvedFiles;
    }

    public Set<File> getFilesForApproval() {
        return nonNull(filesForApproval) ? filesForApproval : Set.of();
    }

    public void setFilesForApproval(Collection<File> filesForApproval) {
        this.filesForApproval = new HashSet<>(filesForApproval);
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
    }

    public void publishApprovedFiles(ResourceService resourceService) {
        getApprovedFiles().forEach(file -> FileEntry.queryObject(file.getIdentifier(), getResourceIdentifier())
                                               .fetch(resourceService)
                                               .ifPresent(fileEntry -> fileEntry.approve(resourceService, new User(
                                                   getFinalizedBy().getValue()))));
    }

    protected void emptyFilesForApproval() {
        this.setFilesForApproval(Set.of());
    }

    public FilesApprovalEntry approveFiles() {
        this.approvedFiles = getFilesForApproval().stream().map(this::toApprovedFile).collect(Collectors.toSet());
        this.filesForApproval = Set.of();
        return this;
    }

    public FilesApprovalEntry withFilesForApproval(Collection<File> filesForApproval) {
        setFilesForApproval(filesForApproval);
        return this;
    }

    private File toApprovedFile(File file) {
        return file instanceof PendingFile<?, ?> pendingFile ? pendingFile.approve() : file;
    }

    public boolean fileIsApproved(File file) {
        return getApprovedFiles().stream().map(File::getIdentifier).toList().contains(file.getIdentifier());
    }

    protected boolean canPublishMetadataAndNoFilesToApprove(PublishingWorkflow workflow) {
        return REGISTRATOR_PUBLISHES_METADATA_ONLY.equals(workflow) && getFilesForApproval().isEmpty();
    }

    protected FilesApprovalEntry handleMetadataOnlyWorkflow(Resource resource, UserInstance userInstance,
                                                            PublishingWorkflow workflow) {
        return canPublishMetadataAndNoFilesToApprove(workflow)
                   ? this.complete(resource.toPublication(), userInstance)
                   : this;
    }
}
