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
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.model.Username;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(DoiRequestDto.TYPE)
public class DoiRequestDto extends TicketDto {

    public static final String TYPE = "DoiRequest";

    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String ID_FIELD = "id";
    public static final String FINALIZED_BY_FIELD = "finalizedBy";

    @JsonProperty(CREATED_DATE_FIELD)
    private final Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private final Instant modifiedDate;
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(ID_FIELD)
    private final URI id;
    @JsonProperty(FINALIZED_BY_FIELD)
    private final Username finalizedBy;

    @SuppressWarnings({"PMD.ExcessiveParameterList"})
    @JsonCreator
    public DoiRequestDto(@JsonProperty(STATUS_FIELD) TicketDtoStatus status,
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
                         @JsonProperty(FINALIZED_BY_FIELD) Username finalizedBy) {
        super(status, messages, viewedBy, assignee, publicationIdentifier, owner, ownerAffiliation);
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.identifier = identifier;
        this.id = id;
        this.finalizedBy = finalizedBy;
    }

    public static TicketDto empty() {
        return new DoiRequestDto(null, null, null, null, null, null, null, null, null, null, null, null);
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

    public Username getFinalizedBy() {
        return finalizedBy;
    }

    @Override
    public Class<? extends TicketEntry> ticketType() {
        return DoiRequest.class;
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getStatus(), getCreatedDate(), getModifiedDate(), getIdentifier(),
                            getPublicationIdentifier(), id, getMessages(), getAssignee(), getOwner(),
                            getOwnerAffiliation(), getFinalizedBy());
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoiRequestDto)) {
            return false;
        }
        DoiRequestDto that = (DoiRequestDto) o;
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
               && Objects.equals(getFinalizedBy(), that.getFinalizedBy());
    }
}
