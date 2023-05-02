package no.unit.nva.publication.ticket;

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
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(GeneralSupportRequestDto.TYPE)
public class GeneralSupportRequestDto extends TicketDto {
    
    public static final String TYPE = "GeneralSupportCase";
    public static final String STATUS_FIELD = "status";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String ID_FIELD = "id";
    @JsonProperty(ID_FIELD)
    private final URI id;
    @JsonProperty(CREATED_DATE_FIELD)
    private final Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private final Instant modifiedDate;
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    
    public GeneralSupportRequestDto(@JsonProperty(STATUS_FIELD) TicketStatus status,
                                    @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                                    @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                                    @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                                    @JsonProperty(PUBLICATION_FIELD) PublicationSummary publicationSummary,
                                    @JsonProperty(ID_FIELD) URI id,
                                    @JsonProperty(MESSAGES_FIELD) List<MessageDto> messages,
                                    @JsonProperty(VIEWED_BY) Set<User> viewedBy,
                                    @JsonProperty(ASSIGNEE_FIELD) Username assignee) {
        super(status, messages, viewedBy, publicationSummary, assignee);
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.identifier = identifier;
        this.id = id;
    }
    
    public static GeneralSupportRequestDto empty() {
        return new GeneralSupportRequestDto(null, null, null, null, null, null, null, null, null);
    }
    
    public URI getId() {
        return id;
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
    
    @JacocoGenerated
    @Override
    public Class<? extends TicketEntry> ticketType() {
        return GeneralSupportRequest.class;
    }
    
    @Override
    public TicketEntry toTicket() {
        var request = new GeneralSupportRequest();
        request.setIdentifier(this.getIdentifier());
        request.setStatus(this.getStatus());
        request.setPublicationDetails(PublicationDetails.create(this.getPublicationSummary()));
        request.setCreatedDate(this.getCreatedDate());
        request.setModifiedDate(this.getModifiedDate());
        request.setViewedBy(this.getViewedBy());
        request.setAssignee(this.getAssignee());
        return request;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getStatus(), getCreatedDate(), getModifiedDate(), getIdentifier(),
            getPublicationSummary().getPublicationId(), getMessages(), getAssignee());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeneralSupportRequestDto)) {
            return false;
        }
        GeneralSupportRequestDto that = (GeneralSupportRequestDto) o;
        return Objects.equals(getId(), that.getId())
               && getStatus() == that.getStatus()
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getPublicationSummary().getPublicationId(),
            that.getPublicationSummary().getPublicationId())
               && Objects.equals(getMessages(), that.getMessages())
               && Objects.equals(getAssignee(), that.getAssignee());
    }
}
