package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.APPROVED_FILES_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.ASSIGNEE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.FILES_FOR_APPROVAL_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_AFFILIATION_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.STATUS_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.WORKFLOW;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
import no.unit.nva.publication.model.storage.FileApprovalThesisDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.service.impl.ResourceService;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.GodClass"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(FilesApprovalThesis.TYPE)
public class FilesApprovalThesis extends TicketEntry {

    public static final String TYPE = "FilesApprovalThesis";
    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedDate;
    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;
    @JsonProperty(WORKFLOW)
    private PublishingWorkflow workflow;
    @JsonProperty(ASSIGNEE_FIELD)
    private Username assignee;
    @JsonProperty(OWNER_AFFILIATION_FIELD)
    private URI ownerAffiliation;
    @JsonProperty(APPROVED_FILES_FIELD)
    private Set<File> approvedFiles;
    @JsonProperty(FILES_FOR_APPROVAL_FIELD)
    private Set<File> filesForApproval;

    public FilesApprovalThesis() {
        super();
    }

    public static FilesApprovalThesis create(Resource resource, UserInstance userInstance,
                                             PublishingWorkflow workflow) {
        var fileApproval = createFileApproval(resource, userInstance, workflow);
        return REGISTRATOR_PUBLISHES_METADATA_AND_FILES.equals(workflow) ? completeAndApproveFiles(resource,
                                                                                                   userInstance,
                                                                                                   fileApproval)
                   : handleMetadataOnlyWorkflow(resource, userInstance, workflow, fileApproval);
    }

    public void validateCreationRequirements(Resource resource) {
        if (!resource.isDegree()) {
            throw new IllegalStateException("FilesApprovalThesis requires publication to be a degree.");
        }
    }

    @Override
    public void validateCreationRequirements(Publication publication) {

    }

    public static FilesApprovalThesis createQueryObject(URI customerId, SortableIdentifier resourceIdentifier) {
        var ticket = new FilesApprovalThesis();
        ticket.setResourceIdentifier(resourceIdentifier);
        ticket.setCustomerId(customerId);
        return ticket;
    }

    @Override
    public void validateCompletionRequirements(Publication publication) {

    }

    @Override
    public FilesApprovalThesis complete(Publication publication, UserInstance userInstance) {
        var completed = (FilesApprovalThesis) super.complete(publication, userInstance);
        completed.emptyFilesForApproval();
        return completed;
    }

    @Override
    public FilesApprovalThesis copy() {
        var copy = new FilesApprovalThesis();
        copy.setIdentifier(this.getIdentifier());
        copy.setResourceIdentifier(this.getResourceIdentifier());
        copy.setStatus(this.getStatus());
        copy.setModifiedDate(this.getModifiedDate());
        copy.setCreatedDate(this.getCreatedDate());
        copy.setCustomerId(this.getCustomerId());
        copy.setOwner(this.getOwner());
        copy.setViewedBy(this.getViewedBy());
        copy.setWorkflow(this.getWorkflow());
        copy.setAssignee(this.getAssignee());
        copy.setOwnerAffiliation(this.getOwnerAffiliation());
        copy.approvedFiles = this.getApprovedFiles().isEmpty() ? Set.of() : this.getApprovedFiles();
        copy.filesForApproval = this.getFilesForApproval().isEmpty() ? Set.of() : this.getFilesForApproval();
        copy.setFinalizedBy(this.getFinalizedBy());
        copy.setFinalizedDate(this.getFinalizedDate());
        copy.setResponsibilityArea(this.getResponsibilityArea());
        return copy;
    }

    @Override
    public TicketStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(TicketStatus ticketStatus) {
        this.status = ticketStatus;
    }

    @Override
    public Username getAssignee() {
        return assignee;
    }

    @Override
    public void setAssignee(Username assignee) {
        this.assignee = assignee;
    }

    @Override
    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }

    @Override
    public void setOwnerAffiliation(URI ownerAffiliation) {
        this.ownerAffiliation = ownerAffiliation;
    }

    @Override
    public TicketDao toDao() {
        return new FileApprovalThesisDao(this);
    }

    @Override
    public void validateAssigneeRequirements(Publication publication) {

    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getCustomerId(), getModifiedDate(), getCreatedDate(),
                            getWorkflow(), getAssignee(), getOwnerAffiliation(), getApprovedFiles(),
                            getFilesForApproval());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FilesApprovalThesis that)) {
            return false;
        }
        return Objects.equals(getIdentifier(), that.getIdentifier()) && getStatus() == that.getStatus() &&
               Objects.equals(getCustomerId(), that.getCustomerId()) &&
               Objects.equals(getModifiedDate(), that.getModifiedDate()) &&
               Objects.equals(getCreatedDate(), that.getCreatedDate()) && getWorkflow() == that.getWorkflow() &&
               Objects.equals(getAssignee(), that.getAssignee()) &&
               Objects.equals(getOwnerAffiliation(), that.getOwnerAffiliation());
    }

    @Override
    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }

    @Override
    public Publication toPublication(ResourceService resourceService) {
        return attempt(() -> resourceService.getPublicationByIdentifier(getResourceIdentifier())).orElseThrow();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public void setCreatedDate(Instant now) {
        this.createdDate = now;
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant now) {
        this.modifiedDate = now;
    }

    @Override
    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @Override
    public String getStatusString() {
        return status.toString();
    }

    public Set<File> getFilesForApproval() {
        return nonNull(filesForApproval) ? filesForApproval : Set.of();
    }

    public void setFilesForApproval(Set<File> filesForApproval) {
        this.filesForApproval = filesForApproval;
    }

    public Set<File> getApprovedFiles() {
        return nonNull(approvedFiles) ? approvedFiles : Set.of();
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    private void setWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
    }

    private static FilesApprovalThesis createFileApproval(Resource resource, UserInstance userInstance,
                                                          PublishingWorkflow workflow) {
        var fileApproval = new FilesApprovalThesis();
        fileApproval.setIdentifier(SortableIdentifier.next());
        fileApproval.setCustomerId(resource.getCustomerId());
        fileApproval.setStatus(TicketStatus.PENDING);
        fileApproval.setViewedBy(Collections.emptySet());
        fileApproval.setResourceIdentifier(resource.getIdentifier());
        fileApproval.setOwnerAffiliation(userInstance.getTopLevelOrgCristinId());
        fileApproval.setResponsibilityArea(userInstance.getPersonAffiliation());
        fileApproval.setOwner(userInstance.getUser());
        fileApproval.setFilesForApproval(resource.getPendingFiles());
        fileApproval.setWorkflow(workflow);
        fileApproval.validateCreationRequirements(resource);
        return fileApproval;
    }

    private static FilesApprovalThesis completeAndApproveFiles(Resource resource, UserInstance userInstance,
                                                               FilesApprovalThesis fileApproval) {
        fileApproval.setAssignee(new Username(userInstance.getUsername()));
        fileApproval.approveFiles();
        return fileApproval.complete(resource.toPublication(), userInstance);
    }

    private static FilesApprovalThesis handleMetadataOnlyWorkflow(Resource resource, UserInstance userInstance,
                                                                  PublishingWorkflow workflow,
                                                                  FilesApprovalThesis fileApproval) {
        return canPublishMetadataAndNoFilesToApprove(workflow, fileApproval) ? fileApproval.complete(
            resource.toPublication(), userInstance) : fileApproval;
    }

    private static boolean canPublishMetadataAndNoFilesToApprove(PublishingWorkflow workflow,
                                                                 FilesApprovalThesis fileApproval) {
        return REGISTRATOR_PUBLISHES_METADATA_ONLY.equals(workflow) && fileApproval.getFilesForApproval().isEmpty();
    }

    private void emptyFilesForApproval() {
        this.filesForApproval = Set.of();
    }

    private void approveFiles() {
        this.approvedFiles = getFilesForApproval().stream().map(this::toApprovedFile).collect(Collectors.toSet());
        this.filesForApproval = Set.of();
    }

    private File toApprovedFile(File file) {
        return file instanceof PendingFile<?, ?> pendingFile ? pendingFile.approve() : file;
    }
}
