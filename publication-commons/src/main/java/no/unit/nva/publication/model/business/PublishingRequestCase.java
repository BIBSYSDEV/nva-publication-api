package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.ASSIGNEE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.IDENTIFIER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.MODIFIED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_AFFILIATION_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.OWNER_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.STATUS_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.WORKFLOW;
import static no.unit.nva.publication.model.business.TicketStatus.COMPLETED;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Publication;
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.UnpublishedFile;
import no.unit.nva.publication.exception.InvalidPublicationException;
import no.unit.nva.publication.model.storage.PublishingRequestDao;
import no.unit.nva.publication.model.storage.TicketDao;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.ConflictException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(PublishingRequestCase.TYPE)
//TODO fix God class
@SuppressWarnings("PMD.GodClass")
public class PublishingRequestCase extends TicketEntry {

    public static final String RESOURCE_LACKS_REQUIRED_DATA = "Resource does not have required data to be "
                                                              + "published: ";

    public static final String TYPE = "PublishingRequestCase";
    public static final String MARKED_FOR_DELETION_ERROR =
        "Publication is marked for deletion and cannot be published.";
    public static final String APPROVED_FILES_FIELD = "approvedFiles";
    public static final String NOT_COMPLETED_PUBLISHING_REQUEST_MESSAGE =
        "Not allowed to set approved files for not Completed PublishingRequest";
    public static final String FILES_FOR_APPROVAL_FIELD = "filesForApproval";

    @JsonProperty(IDENTIFIER_FIELD)
    private SortableIdentifier identifier;
    @JsonProperty(STATUS_FIELD)
    private TicketStatus status;
    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(OWNER_FIELD)
    private User owner;
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
    private Set<UUID> approvedFiles;
    @JsonProperty(FILES_FOR_APPROVAL_FIELD)
    private Set<FileForApproval> filesForApproval;

    public PublishingRequestCase() {
        super();
    }

    public static PublishingRequestCase createOpeningCaseObject(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        var openingCaseObject = new PublishingRequestCase();
        openingCaseObject.setOwner(userInstance.getUser());
        openingCaseObject.setCustomerId(userInstance.getCustomerId());
        openingCaseObject.setStatus(TicketStatus.PENDING);
        openingCaseObject.setViewedBy(ViewedBy.addAll(openingCaseObject.getOwner()));
        openingCaseObject.setResourceIdentifier(publication.getIdentifier());
        openingCaseObject.setFilesForApproval(extractFilesForApproval(publication));
        return openingCaseObject;
    }

    private static Set<FileForApproval> extractFilesForApproval(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(UnpublishedFile.class::isInstance)
                   .map(UnpublishedFile.class::cast)
                   .map(FileForApproval::fromFile)
                   .collect(Collectors.toSet());
    }

    public static PublishingRequestCase createQueryObject(UserInstance userInstance,
                                                          SortableIdentifier publicationIdentifier,
                                                          SortableIdentifier publishingRequestIdentifier) {
        return createPublishingRequestIdentifyingObject(
            userInstance,
            publicationIdentifier,
            publishingRequestIdentifier);
    }

    public static PublishingRequestCase createQueryObject(SortableIdentifier resourceIdentifier, URI customerId) {
        var queryObject = new PublishingRequestCase();
        queryObject.setResourceIdentifier(resourceIdentifier);
        queryObject.setCustomerId(customerId);
        return queryObject;
    }

    public static void assertThatPublicationHasMinimumMandatoryFields(Publication resource)
        throws InvalidPublicationException {

        if (!resource.isPublishable()) {
            throwErrorWhenPublishingResourceThatDoesNotHaveRequiredData(resource);
        }
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    public void setWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public void validateCreationRequirements(Publication publication)
        throws ConflictException {
        if (PublicationStatus.DRAFT_FOR_DELETION == publication.getStatus()) {
            throw new ConflictException(MARKED_FOR_DELETION_ERROR);
        }
    }

    @Override
    public void validateCompletionRequirements(Publication publication) {
    }

    @Override
    public PublishingRequestCase complete(Publication publication, Username finalizedBy) {
        var completed = (PublishingRequestCase) super.complete(publication, finalizedBy);
        completed.setApprovedFiles(getFilesToApprove(publication));
        completed.emptyFilesForApproval();
        return completed;
    }

    private static Set<UUID> getFilesToApprove(Publication publication) {
        return publication.getAssociatedArtifacts().stream()
                   .filter(UnpublishedFile.class::isInstance)
                   .map(UnpublishedFile.class::cast)
                   .map(UnpublishedFile::getIdentifier)
                   .collect(Collectors.toSet());
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
        return copy;
    }

    @Override
    public TicketStatus getStatus() {
        return status;
    }

    @Override
    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    @Override
    public Username getAssignee() {
        return assignee;
    }

    @Override
    public void setAssignee(Username assignee) {
        this.assignee = assignee;
    }

    public Set<FileForApproval> getFilesForApproval() {
        return nonNull(filesForApproval) ? filesForApproval : Set.of();
    }

    public void setFilesForApproval(Set<FileForApproval> filesForApproval) {
        this.filesForApproval = filesForApproval;
    }

    public void emptyFilesForApproval() {
        this.filesForApproval = Set.of();
    }

    @Override
    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }

    @Override
    public void setOwnerAffiliation(URI ownerAffiliation) {
        this.ownerAffiliation = ownerAffiliation;
    }

    public Set<UUID> getApprovedFiles() {
        return nonNull(approvedFiles) ? approvedFiles : Collections.emptySet();
    }

    /**
     * This method should be used ONLY when completing/approving PublishingRequestCase.
     *
     * @param approvedFiles Set of UUIDs representing the files that have been published
     *                      when approving PublishingRequestCase.
     *
     * @throws UnsupportedOperationException if this method is called when
     *     PublishingRequestCase is not in the Completed status
     */

    public void setApprovedFiles(Set<UUID> approvedFiles) {
        if (settingApprovedFilesForNotCompletedStatus(approvedFiles)) {
            throw new UnsupportedOperationException(NOT_COMPLETED_PUBLISHING_REQUEST_MESSAGE);
        }
        this.approvedFiles = approvedFiles;
    }

    private boolean settingApprovedFilesForNotCompletedStatus(Set<UUID> approvedFiles) {
        return !approvedFiles.isEmpty() && !COMPLETED.equals(getStatus());
    }

    @Override
    public void validateAssigneeRequirements(Publication publication) {
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
        return attempt(() -> resourceService.getPublicationByIdentifier(getResourceIdentifier()))
                   .orElseThrow();
    }

    @Override
    public String getType() {
        return PublishingRequestCase.TYPE;
    }

    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public Instant getModifiedDate() {
        return modifiedDate;
    }

    @Override
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    @Override
    public User getOwner() {
        return owner;
    }

    @Override
    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    @Override
    public TicketDao toDao() {
        return new PublishingRequestDao(this);
    }

    @Override
    public String getStatusString() {
        return status.toString();
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getCustomerId(), getOwner(), getModifiedDate(),
                            getCreatedDate(), getAssignee(), getWorkflow(), getOwnerAffiliation(),
                            getApprovedFiles());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestCase)) {
            return false;
        }
        PublishingRequestCase that = (PublishingRequestCase) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && getStatus() == that.getStatus()
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getWorkflow(), that.getWorkflow())
               && Objects.equals(getAssignee(), that.getAssignee())
               && Objects.equals(getOwnerAffiliation(), that.getOwnerAffiliation())
               && Objects.equals(getApprovedFiles(), that.getApprovedFiles());
    }

    public PublishingRequestCase persistAutoComplete(TicketService ticketService, Publication publication)
        throws ApiGatewayException {
        var completed = this.complete(publication, publication.getResourceOwner().getOwner());
        return ticketService.createTicket(completed);
    }

    private static PublishingRequestCase createPublishingRequestIdentifyingObject(
        UserInstance userInstance,
        SortableIdentifier publicationIdentifier,
        SortableIdentifier publishingRequestIdentifier) {

        var newPublishingRequest = new PublishingRequestCase();
        newPublishingRequest.setOwner(userInstance.getUser());
        newPublishingRequest.setCustomerId(userInstance.getCustomerId());
        newPublishingRequest.setResourceIdentifier(publicationIdentifier);
        newPublishingRequest.setIdentifier(publishingRequestIdentifier);
        return newPublishingRequest;
    }

    private static void throwErrorWhenPublishingResourceThatDoesNotHaveRequiredData(Publication resource)
        throws InvalidPublicationException {
        throw new InvalidPublicationException(RESOURCE_LACKS_REQUIRED_DATA + resource.getIdentifier().toString());
    }
}
