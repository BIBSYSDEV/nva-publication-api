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
import java.util.Optional;
import java.util.Set;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublishingRequestDto.TYPE)
public class PublishingRequestDto extends TicketDto {
    
    public static final String TYPE = "PublishingRequest";
    
    public static final String STATUS_FIELD = "status";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String ID_FIELD = "id";
    
    @JsonProperty(CREATED_DATE_FIELD)
    private final Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private final Instant modifiedDate;
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(ID_FIELD)
    private final URI id;
    
    @JsonCreator
    public PublishingRequestDto(@JsonProperty(STATUS_FIELD) TicketStatus status,
                                @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                                @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                                @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                                @JsonProperty(PUBLICATION_FIELD) PublicationSummary publicationSummary,
                                @JsonProperty(ID_FIELD) URI id,
                                @JsonProperty(MESSAGES_FIELD) List<MessageDto> messages,
                                @JsonProperty(VIEWED_BY) Set<User> viewedBy,
                                @JsonProperty(ASSIGNEE_FIELD) Optional<User> assignee) {
        super(status, messages, viewedBy, publicationSummary, assignee);
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.identifier = identifier;
        this.id = id;
    }
    
    public static TicketDto empty() {
        return new PublishingRequestDto(null, null, null, null, null, null, null, null, null);
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
    
    @Override
    public Class<? extends TicketEntry> ticketType() {
        return PublishingRequestCase.class;
    }
    
    @Override
    public TicketEntry toTicket() {
        var ticket = new PublishingRequestCase();
        ticket.setCreatedDate(getCreatedDate());
        ticket.setStatus(getStatus());
        ticket.setModifiedDate(getModifiedDate());
        ticket.setIdentifier(getIdentifier());
        ticket.setPublicationDetails(PublicationDetails.create(getPublicationSummary()));
        ticket.setViewedBy(getViewedBy());
        return ticket;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getStatus(), getCreatedDate(), getModifiedDate(), getIdentifier(),
            getPublicationSummary().getPublicationId(), id, getMessages(), getAssignee());
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
               && Objects.equals(getPublicationSummary().getPublicationId(),
            that.getPublicationSummary().getPublicationId())
               && Objects.equals(id, that.id)
               && Objects.equals(getMessages(), that.getMessages())
               && Objects.equals(getAssignee(), that.getAssignee());
    }
}
