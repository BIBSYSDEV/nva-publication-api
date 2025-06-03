package no.unit.nva.publication.ticket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.TicketOperation;
import no.unit.nva.model.Username;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublishingRequestDto.TYPE)
public class PublishingRequestDto extends TicketDto {

    public static final String TYPE = "PublishingRequest";

    private static final String WORKFLOW_FIELD = "workflow";
    private static final String APPROVED_FILES_FIELD = "approvedFiles";
    private static final String FILES_FOR_APPROVAL = "filesForApproval";

    @JsonProperty(WORKFLOW_FIELD)
    private final PublishingWorkflow workflow;
    @JsonProperty(APPROVED_FILES_FIELD)
    private final Set<File> approvedFiles;
    @JsonProperty(FILES_FOR_APPROVAL)
    private final Set<File> filesForApproval;

    @SuppressWarnings({"PMD.ExcessiveParameterList"})
    @JsonCreator
    public PublishingRequestDto(@JsonProperty(STATUS_FIELD) TicketDtoStatus status,
                                @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                                @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                                @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                                @JsonProperty(PUBLICATION_IDENTIFIER_FIELD) SortableIdentifier publicationIdentifier,
                                @JsonProperty(MESSAGES_FIELD) List<MessageDto> messages,
                                @JsonProperty(VIEWED_BY) Set<User> viewedBy,
                                @JsonProperty(ASSIGNEE_FIELD) Username assignee,
                                @JsonProperty(OWNER_FIELD) User owner,
                                @JsonProperty(OWNER_AFFILIATION_FIELD) URI ownerAffiliation,
                                @JsonProperty(WORKFLOW_FIELD) PublishingWorkflow workflow,
                                @JsonProperty(APPROVED_FILES_FIELD) Set<File> approvedFiles,
                                @JsonProperty(FILES_FOR_APPROVAL) Set<File> filesForApproval,
                                @JsonProperty(FINALIZED_BY_FIELD) Username finalizedBy,
                                @JsonProperty(FINALIZED_DATE_FIELD) Instant finalizedDate,
                                @JsonProperty(AVAILABLE_INSTITUTIONS_FIELD) Collection<URI> availableInstitutions,
                                @JsonProperty(ALLOWED_OPERATIONS_FIELD) Set<TicketOperation> allowedOperations) {
        super(identifier,
              status,
              messages,
              viewedBy,
              assignee,
              publicationIdentifier,
              owner,
              ownerAffiliation,
              finalizedBy,
              finalizedDate, createdDate, modifiedDate, availableInstitutions, allowedOperations);
        this.workflow = workflow;
        this.approvedFiles = approvedFiles;
        this.filesForApproval = filesForApproval;
    }

    public static TicketDto empty() {
        return new PublishingRequestDto(null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        Collections.emptySet(),
                                        Collections.emptySet(),
                                        null,
                                        null,
                                        Collections.emptySet(),
                                        Collections.emptySet());
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    public Set<File> getApprovedFiles() {
        return approvedFiles;
    }

    public Set<File> getFilesForApproval() {
        return filesForApproval;
    }

    @Override
    public Class<? extends TicketEntry> ticketType() {
        return PublishingRequestCase.class;
    }

    @JacocoGenerated
    private Set<TicketOperation> getAllowedOperations() {
        return allowedOperations;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getStatus(), getCreatedDate(), getModifiedDate(), getIdentifier(),
                            getPublicationIdentifier(), getId(), getMessages(), getAssignee(), getOwner(),
                            getOwnerAffiliation(), getWorkflow(), getApprovedFiles(), getFilesForApproval(),
                            getFinalizedBy(), getFinalizedDate(), getAllowedOperations());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestDto that)) {
            return false;
        }
        return getStatus() == that.getStatus()
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getPublicationIdentifier(), that.getPublicationIdentifier())
               && Objects.equals(getId(), that.getId())
               && Objects.equals(getMessages(), that.getMessages())
               && Objects.equals(getAssignee(), that.getAssignee())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getOwnerAffiliation(), that.getOwnerAffiliation())
               && Objects.equals(getWorkflow(), that.getWorkflow())
               && Objects.equals(getApprovedFiles(), that.getApprovedFiles())
               && Objects.equals(getFilesForApproval(), that.getFilesForApproval())
               && Objects.equals(getFinalizedBy(), that.getFinalizedBy())
               && Objects.equals(getFinalizedDate(), that.getFinalizedDate())
               && Objects.equals(getAllowedOperations(), that.getAllowedOperations());
    }
}
