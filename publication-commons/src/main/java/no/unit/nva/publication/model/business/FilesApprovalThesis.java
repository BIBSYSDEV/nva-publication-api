package no.unit.nva.publication.model.business;

import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.storage.FileApprovalThesisDao;
import no.unit.nva.publication.model.storage.TicketDao;
import nva.commons.core.JacocoGenerated;

@SuppressWarnings({"PMD.GodClass"})
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(FilesApprovalThesis.TYPE)
public class FilesApprovalThesis extends FilesApprovalEntry {

    public static final String TYPE = "FilesApprovalThesis";

    public FilesApprovalThesis() {
        super();
    }

    public static FilesApprovalThesis create(Resource resource, UserInstance userInstance,
                                             PublishingWorkflow workflow) {
        var fileApproval = createFileApproval(resource, userInstance, workflow);
        return REGISTRATOR_PUBLISHES_METADATA_AND_FILES.equals(workflow)
                   ? (FilesApprovalThesis) fileApproval.completeAndApproveFiles(resource, userInstance)
                   : (FilesApprovalThesis) fileApproval.handleMetadataOnlyWorkflow(resource, userInstance, workflow);
    }

    public static FilesApprovalThesis createQueryObject(URI customerId, SortableIdentifier resourceIdentifier) {
        var ticket = new FilesApprovalThesis();
        ticket.setResourceIdentifier(resourceIdentifier);
        ticket.setCustomerId(customerId);
        return ticket;
    }

    @Override
    public void validateCreationRequirements(Publication publication) {
        if (!Resource.fromPublication(publication).isDegree()) {
            throw new IllegalStateException("FilesApprovalThesis requires publication to be a degree.");
        }
    }

    @Override
    public void validateCompletionRequirements(Publication publication) {
        // NO OP
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
        copy.setApprovedFiles(this.getApprovedFiles().isEmpty() ? Set.of() : this.getApprovedFiles());
        copy.setFilesForApproval(this.getFilesForApproval().isEmpty() ? Set.of() : this.getFilesForApproval());
        copy.setFinalizedBy(this.getFinalizedBy());
        copy.setFinalizedDate(this.getFinalizedDate());
        copy.setResponsibilityArea(this.getResponsibilityArea());
        return copy;
    }

    @Override
    public TicketDao toDao() {
        return new FileApprovalThesisDao(this);
    }

    @Override
    public String getType() {
        return TYPE;
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
        fileApproval.validateCreationRequirements(resource.toPublication());
        return fileApproval;
    }
}
