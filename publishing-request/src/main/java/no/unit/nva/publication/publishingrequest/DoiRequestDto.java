package no.unit.nva.publication.publishingrequest;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(DoiRequestDto.TYPE)
public class DoiRequestDto extends TicketDto {
    
    public static final String TYPE = "DoiRequest";
    public static final String STATUS_FIELD = "status";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String PUBLICATION_ID_FIELD = "publicationId";
    public static final String ID_FIELD = "id";
    
    @JsonProperty(STATUS_FIELD)
    private final TicketStatus status;
    @JsonProperty(CREATED_DATE_FIELD)
    private final Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private final Instant modifiedDate;
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(PUBLICATION_ID_FIELD)
    private final URI publicationId;
    @JsonProperty(ID_FIELD)
    private final URI id;
    private final List<MessageDto> messages;
    
    @JsonCreator
    public DoiRequestDto(@JsonProperty(STATUS_FIELD) TicketStatus status,
                         @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                         @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                         @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                         @JsonProperty(PUBLICATION_ID_FIELD) URI publicationId,
                         @JsonProperty(ID_FIELD) URI id,
                         @JsonProperty(MESSAGES_FIELD) List<MessageDto> messages) {
        super();
        this.status = status;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.identifier = identifier;
        this.publicationId = publicationId;
        this.id = id;
        this.messages = messages;
    }
    
    public static TicketDto empty() {
        return new DoiRequestDto(null, null, null, null, null, null, null);
    }
    
    @Override
    public TicketStatus getStatus() {
        return status;
    }
    
    @Override
    public List<MessageDto> getMessages() {
        return nonNull(messages) ? messages : Collections.emptyList();
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
    
    public URI getPublicationId() {
        return publicationId;
    }
    
    @Override
    public Class<? extends TicketEntry> ticketType() {
        return DoiRequest.class;
    }
    
    @Override
    public TicketEntry toTicket() {
        var ticket = new DoiRequest();
        ticket.setCreatedDate(getCreatedDate());
        ticket.setStatus(getStatus());
        ticket.setModifiedDate(getModifiedDate());
        ticket.setIdentifier(getIdentifier());
        ticket.setResourceIdentifier(extractResourceIdentifier(getPublicationId()));
        return ticket;
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
               && Objects.equals(getPublicationId(), that.getPublicationId())
               && Objects.equals(id, that.id)
               && Objects.equals(getMessages(), that.getMessages());
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getStatus(), getCreatedDate(), getModifiedDate(), getIdentifier(),
            getPublicationId(), id, getMessages());
    }
}
