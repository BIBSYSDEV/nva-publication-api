package no.unit.nva.publication.ticket;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.PublishingWorkflow;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublishingRequestDto.TYPE)
@SuppressWarnings("PMD.ExcessiveParameterList")
public class PublishingRequestDto extends TicketDto {

    public static final String TYPE = "PublishingRequest";

    public static final String STATUS_FIELD = "status";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String ID_FIELD = "id";
    public static final String WORKFLOW_FIELD = "workflow";
    public static final String APPROVED_FILES_FIELD = "approvedFiles";

    @JsonProperty(CREATED_DATE_FIELD)
    private final Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private final Instant modifiedDate;
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(ID_FIELD)
    private final URI id;
    @JsonProperty(WORKFLOW_FIELD)
    private final PublishingWorkflow workflow;
    @JsonProperty(APPROVED_FILES_FIELD)
    private final Set<UUID> approvedFiles;

    @JsonCreator
    public PublishingRequestDto(@JsonProperty(STATUS_FIELD) TicketDtoStatus status,
                                @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                                @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                                @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                                @JsonProperty(PUBLICATION_IDENTIFIER_FIELD) SortableIdentifier publicationIdentifier,
                                @JsonProperty(ID_FIELD) URI id,
                                @JsonProperty(MESSAGES_FIELD) List<MessageDto> messages,
                                @JsonProperty(VIEWED_BY) Set<User> viewedBy,
                                @JsonProperty(ASSIGNEE_FIELD) Username assignee,
                                @JsonProperty(OWNER_FIELD) User owner,
                                @JsonProperty(OWNER_AFFILIATION_FIELD) URI ownerAffiliation,
                                @JsonProperty(WORKFLOW_FIELD) PublishingWorkflow workflow,
                                @JsonProperty(APPROVED_FILES_FIELD) Set<UUID> approvedFiles,
                                @JsonProperty(FINALIZED_BY_FIELD) Username finalizedBy) {
        super(status, messages, viewedBy, assignee, publicationIdentifier, owner, ownerAffiliation, finalizedBy);
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.identifier = identifier;
        this.id = id;
        this.workflow = workflow;
        this.approvedFiles = approvedFiles;
    }

    public static TicketDto empty() {
        return new PublishingRequestDto(null, null, null, null, null, null, null, null, null, null, null, null, Set.of(), null);
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public SortableIdentifier getIdentifier() {
        return identifier;
    }

    public PublishingWorkflow getWorkflow() {
        return workflow;
    }

    public Set<UUID> getApprovedFiles() {
        return approvedFiles;
    }

    @Override
    public Class<? extends TicketEntry> ticketType() {
        return PublishingRequestCase.class;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getStatus(), getCreatedDate(), getModifiedDate(), getIdentifier(),
                            getPublicationIdentifier(), id, getMessages(), getAssignee(), getOwner(),
                            getOwnerAffiliation(), getWorkflow(), getApprovedFiles(), getFinalizedBy());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublishingRequestDto)) {
            return false;
        }
        PublishingRequestDto that = (PublishingRequestDto) o;
        return getStatus() == that.getStatus()
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getPublicationIdentifier(), that.getPublicationIdentifier())
               && Objects.equals(id, that.id)
               && Objects.equals(getMessages(), that.getMessages())
               && Objects.equals(getAssignee(), that.getAssignee())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getOwnerAffiliation(), that.getOwnerAffiliation())
               && Objects.equals(getWorkflow(), that.getWorkflow())
               && Objects.equals(getApprovedFiles(), that.getApprovedFiles())
               && Objects.equals(getFinalizedBy(), that.getFinalizedBy());
    }
}
