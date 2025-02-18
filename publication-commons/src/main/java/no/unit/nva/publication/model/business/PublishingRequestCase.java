package no.unit.nva.publication.model.business;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES;
import static no.unit.nva.publication.model.business.PublishingWorkflow.REGISTRATOR_PUBLISHES_METADATA_ONLY;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.ASSIGNEE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CREATED_DATE_FIELD;
import static no.unit.nva.publication.model.business.TicketEntry.Constants.CUSTOMER_ID_FIELD;
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
import no.unit.nva.model.PublicationStatus;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.PendingFile;
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

    public static final String RESOURCE_LACKS_REQUIRED_DATA =
        "Resource does not have required data to be " + "published: ";
    public static final String TYPE = "PublishingRequestCase";
    public static final String MARKED_FOR_DELETION_ERROR =
        "Publication is marked for deletion and cannot be " + "published.";
    public static final String APPROVED_FILES_FIELD = "approvedFiles";
    public static final String FILES_FOR_APPROVAL_FIELD = "filesForApproval";
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

    public PublishingRequestCase() {
        super();
    }

    public static PublishingRequestCase fromPublication(Publication publication) {
        var userInstance = UserInstance.fromPublication(publication);
        var openingCaseObject = new PublishingRequestCase();
        openingCaseObject.setCustomerId(userInstance.getCustomerId());
        openingCaseObject.setStatus(TicketStatus.PENDING);
        openingCaseObject.setViewedBy(ViewedBy.addAll(userInstance.getUser()));
        openingCaseObject.setResourceIdentifier(publication.getIdentifier());
        return openingCaseObject;
    }

    public static PublishingRequestCase create(Resource resource, UserInstance userInstance,
                                               PublishingWorkflow workflow) {
        var publishingRequestCase = createPublishingRequest(resource, userInstance, workflow);
        return REGISTRATOR_PUBLISHES_METADATA_AND_FILES.equals(workflow)
                   ? completePublishingRequestAndApproveFiles(resource, userInstance, publishingRequestCase)
                   : handleMetadataOnlyWorkflow(resource, userInstance, workflow, publishingRequestCase);
    }

    private static PublishingRequestCase handleMetadataOnlyWorkflow(Resource resource, UserInstance userInstance,
                                                                  PublishingWorkflow workflow,
                                                                  PublishingRequestCase publishingRequestCase) {
        return canPublishMetadataAndNoFilesToApprove(workflow, publishingRequestCase)
                   ? publishingRequestCase.complete(resource.toPublication(), userInstance)
                   : publishingRequestCase;
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
    public void validateCreationRequirements(Publication publication) throws ConflictException {
        if (PublicationStatus.DRAFT_FOR_DELETION == publication.getStatus()) {
            throw new ConflictException(MARKED_FOR_DELETION_ERROR);
        }
    }

    @Override
    public void validateCompletionRequirements(Publication publication) {
    }

    @Override
    public PublishingRequestCase complete(Publication publication, UserInstance userInstance) {
        var completed = (PublishingRequestCase) super.complete(publication, userInstance);
        completed.emptyFilesForApproval();
        return completed;
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
        copy.approvedFiles = this.getApprovedFiles().isEmpty() ? Set.of() : this.getApprovedFiles();
        copy.filesForApproval = this.getFilesForApproval().isEmpty() ? Set.of() : this.getFilesForApproval();
        copy.setFinalizedBy(this.getFinalizedBy());
        copy.setFinalizedDate(this.getFinalizedDate());
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

    @Override
    public URI getOwnerAffiliation() {
        return ownerAffiliation;
    }

    @Override
    public void setOwnerAffiliation(URI ownerAffiliation) {
        this.ownerAffiliation = ownerAffiliation;
    }

    @Override
    public void validateAssigneeRequirements(Publication publication) {
    }

    @Override
    public PublishingRequestCase withOwnerAffiliation(URI ownerAffiliation) {
        this.setOwnerAffiliation(ownerAffiliation);
        return this;
    }

    public Set<File> getFilesForApproval() {
        return nonNull(filesForApproval) ? filesForApproval : Set.of();
    }

    public void setFilesForApproval(Set<File> filesForApproval) {
        this.filesForApproval = filesForApproval;
    }

    public void emptyFilesForApproval() {
        this.filesForApproval = Set.of();
    }

    public Set<File> getApprovedFiles() {
        return nonNull(approvedFiles) ? approvedFiles : Collections.emptySet();
    }

    public void setApprovedFiles(Set<File> approvedFiles) {
        this.approvedFiles = approvedFiles;
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

    public PublishingRequestCase withFilesForApproval(Set<File> filesForApproval) {
        this.filesForApproval = filesForApproval;
        return this;
    }

    public PublishingRequestCase publishApprovedFile() {
        this.approvedFiles = getFilesForApproval().stream().map(this::toApprovedFile).collect(Collectors.toSet());
        this.filesForApproval = Set.of();
        return this;
    }

    public void publishApprovedFiles(ResourceService resourceService) {
        getApprovedFiles().forEach(file -> FileEntry.queryObject(file.getIdentifier(), getResourceIdentifier())
                                               .fetch(resourceService)
                                               .ifPresent(fileEntry -> fileEntry.approve(resourceService, new User(
                                                   getFinalizedBy().getValue()))));
    }

    public void rejectRejectedFiles(ResourceService resourceService) {
        getFilesForApproval().stream()
            .map(PendingFile.class::cast)
            .forEach(file -> FileEntry.queryObject(file.getIdentifier(), getResourceIdentifier())
                                 .fetch(resourceService)
                                 .ifPresent(fileEntry -> fileEntry.reject(resourceService,
                                                                          new User(getFinalizedBy().getValue()))));
    }

    public PublishingRequestCase withWorkflow(PublishingWorkflow workflow) {
        this.workflow = workflow;
        return this;
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

    public PublishingRequestCase persistAutoComplete(TicketService ticketService, Publication publication,
                                                     UserInstance userInstance) throws ApiGatewayException {
        return (PublishingRequestCase) this.complete(publication, userInstance).persistNewTicket(ticketService);
    }

    public boolean fileIsApproved(File file) {
        return getApprovedFiles().stream().map(File::getIdentifier).toList().contains(file.getIdentifier());
    }

    private static boolean canPublishMetadataAndNoFilesToApprove(PublishingWorkflow workflow,
                                                                 PublishingRequestCase publishingRequestCase) {
        return REGISTRATOR_PUBLISHES_METADATA_ONLY.equals(workflow) &&
               publishingRequestCase.getFilesForApproval().isEmpty();
    }

    private static PublishingRequestCase completePublishingRequestAndApproveFiles(Resource resource,
                                                                                  UserInstance userInstance,
                                                                                  PublishingRequestCase publishingRequestCase) {
        publishingRequestCase.setAssignee(new Username(userInstance.getUsername()));
        publishingRequestCase.publishApprovedFile();
        return publishingRequestCase.complete(resource.toPublication(), userInstance);
    }

    private static PublishingRequestCase createPublishingRequest(Resource resource, UserInstance userInstance,
                                                                 PublishingWorkflow workflow) {
        var publishingRequestCase = new PublishingRequestCase();
        publishingRequestCase.setIdentifier(SortableIdentifier.next());
        publishingRequestCase.setCustomerId(resource.getCustomerId());
        publishingRequestCase.setStatus(TicketStatus.PENDING);
        publishingRequestCase.setViewedBy(ViewedBy.addAll(userInstance.getUser()));
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

    private static void throwErrorWhenPublishingResourceThatDoesNotHaveRequiredData(Publication resource)
        throws InvalidPublicationException {
        throw new InvalidPublicationException(RESOURCE_LACKS_REQUIRED_DATA + resource.getIdentifier().toString());
    }

    private File toApprovedFile(File file) {
        return file instanceof PendingFile<?, ?> pendingFile ? pendingFile.approve() : file;
    }
}
