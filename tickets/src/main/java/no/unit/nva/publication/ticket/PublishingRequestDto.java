package no.unit.nva.publication.ticket;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.PublicationWorkflow;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.PublicationDetails;
import no.unit.nva.publication.model.business.User;
import nva.commons.core.JacocoGenerated;

import java.beans.ConstructorProperties;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublishingRequestDto.TYPE)
public class PublishingRequestDto extends TicketDto {
    public static final String TYPE = "PublishingRequest";

    private final Instant createdDate;
    private final Instant modifiedDate;
    private final SortableIdentifier identifier;
    private final URI id;
    private final PublicationWorkflow workflow;

    @ConstructorProperties({"status","createdDate","modifiedDate","identifier","publication","messages",
            "id", "viewedBy", "publicationWorkflow", "assignee"})
    public PublishingRequestDto(TicketStatus status,
                                Instant createdDate,
                                Instant modifiedDate,
                                SortableIdentifier identifier,
                                PublicationSummary publication,
                                URI id,
                                List<MessageDto> messages,
                                Set<User> viewedBy,
                                PublicationWorkflow publicationWorkflow,
                                User assignee) {
        super(status, messages, viewedBy, publication,assignee);
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.identifier = identifier;
        this.id = id;
        this.workflow = publicationWorkflow;
    }
    
    public static TicketDto empty() {
        return new PublishingRequestDto(null, null, null, null,null, null, null, null, PublicationWorkflow.UNSET,null);
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

    public PublicationWorkflow getWorkflow() {
        return workflow;
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
        ticket.setWorkflow(getWorkflow());
        return ticket;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getStatus(), getCreatedDate(), getModifiedDate(), getIdentifier(),
            getWorkflow(),getPublicationSummary().getPublicationId(), id, getMessages(), getAssignee());
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
               && Objects.equals(getWorkflow(), that.getWorkflow())
               && Objects.equals(id, that.id)
               && Objects.equals(getMessages(), that.getMessages())
               && Objects.equals(getAssignee(), that.getAssignee());
    }
}
