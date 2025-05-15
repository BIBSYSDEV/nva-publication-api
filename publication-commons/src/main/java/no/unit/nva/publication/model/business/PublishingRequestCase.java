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
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.FilesApprovalEntry;
import no.unit.nva.publication.model.storage.PublishingRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(PublishingRequestCase.TYPE)
public class PublishingRequestCase extends FilesApprovalEntry {

    public static final String TYPE = "PublishingRequestCase";
    public static final String MARKED_FOR_DELETION_ERROR =
        "Publication is marked for deletion and cannot be published.";

    public PublishingRequestCase() {
        super();
    }

    public static PublishingRequestCase create(Resource resource, UserInstance userInstance,
                                               PublishingWorkflow workflow) {
        var publishingRequestCase = createPublishingRequest(resource, userInstance, workflow);
        return REGISTRATOR_PUBLISHES_METADATA_AND_FILES.equals(workflow)
                   ? (PublishingRequestCase) publishingRequestCase.completeAndApproveFiles(resource, userInstance)
                   : (PublishingRequestCase) publishingRequestCase.handleMetadataOnlyWorkflow(resource, userInstance,
                                                                                              workflow);
    }

    public static PublishingRequestCase createWithFilesForApproval(Resource resource, UserInstance userInstance,
                                                                   PublishingWorkflow workflow,
                                                                   Set<File> filesForApproval) {
        var publishingRequestCase = create(resource, userInstance, workflow);
        publishingRequestCase.setFilesForApproval(filesForApproval);
        return publishingRequestCase;
    }

    public static PublishingRequestCase createQueryObject(UserInstance userInstance,
                                                          SortableIdentifier publicationIdentifier,
                                                          SortableIdentifier publishingRequestIdentifier) {
        return createPublishingRequestIdentifyingObject(userInstance, publicationIdentifier,
                                                        publishingRequestIdentifier);
    }

    public static PublishingRequestCase createQueryObject(SortableIdentifier resourceIdentifier, URI customerId) {
        var queryObject = new PublishingRequestCase();
        queryObject.setResourceIdentifier(resourceIdentifier);
        queryObject.setCustomerId(customerId);
        return queryObject;
    }

    @Override
    public void validateCreationRequirements(Publication publication) throws ConflictException {
        if (PublicationStatus.DRAFT_FOR_DELETION == publication.getStatus()) {
            throw new ConflictException(MARKED_FOR_DELETION_ERROR);
        }
    }

    @Override
    public PublishingRequestCase copy() {
        var copy = new PublishingRequestCase();
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
        return new PublishingRequestDao(this);
    }

    @Override
    public String getType() {
        return PublishingRequestCase.TYPE;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getCustomerId(), getOwner(), getModifiedDate(),
                            getCreatedDate(), getAssignee(), getWorkflow(), getOwnerAffiliation(), getApprovedFiles());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestCase that)) {
            return false;
        }
        return Objects.equals(getIdentifier(), that.getIdentifier()) && getStatus() == that.getStatus() &&
               Objects.equals(getCustomerId(), that.getCustomerId()) && Objects.equals(getOwner(), that.getOwner()) &&
               Objects.equals(getModifiedDate(), that.getModifiedDate()) &&
               Objects.equals(getCreatedDate(), that.getCreatedDate()) &&
               Objects.equals(getWorkflow(), that.getWorkflow()) && Objects.equals(getAssignee(), that.getAssignee()) &&
               Objects.equals(getOwnerAffiliation(), that.getOwnerAffiliation()) &&
               Objects.equals(getApprovedFiles(), that.getApprovedFiles());
    }

    @Override
    public void validateCompletionRequirements(Publication publication) {
        // NO OP
    }

    @Override
    public PublishingRequestCase withOwnerAffiliation(URI ownerAffiliation) {
        this.setOwnerAffiliation(ownerAffiliation);
        return this;
    }

    private static PublishingRequestCase createPublishingRequest(Resource resource, UserInstance userInstance,
                                                                 PublishingWorkflow workflow) {
        var publishingRequestCase = new PublishingRequestCase();
        publishingRequestCase.setIdentifier(SortableIdentifier.next());
        publishingRequestCase.setCustomerId(resource.getCustomerId());
        publishingRequestCase.setStatus(TicketStatus.PENDING);
        publishingRequestCase.setViewedBy(Collections.emptySet());
        publishingRequestCase.setResourceIdentifier(resource.getIdentifier());
        publishingRequestCase.setOwnerAffiliation(userInstance.getTopLevelOrgCristinId());
        publishingRequestCase.setResponsibilityArea(userInstance.getPersonAffiliation());
        publishingRequestCase.setOwner(userInstance.getUser());
        publishingRequestCase.setFilesForApproval(resource.getPendingFiles());
        publishingRequestCase.setWorkflow(workflow);
        return publishingRequestCase;
    }

    private static PublishingRequestCase createPublishingRequestIdentifyingObject(UserInstance userInstance,
                                                                                  SortableIdentifier publicationIdentifier,
                                                                                  SortableIdentifier publishingRequestIdentifier) {
        var newPublishingRequest = new PublishingRequestCase();
        newPublishingRequest.setOwner(userInstance.getUser());
        newPublishingRequest.setCustomerId(userInstance.getCustomerId());
        newPublishingRequest.setResourceIdentifier(publicationIdentifier);
        newPublishingRequest.setIdentifier(publishingRequestIdentifier);
        return newPublishingRequest;
    }
}
