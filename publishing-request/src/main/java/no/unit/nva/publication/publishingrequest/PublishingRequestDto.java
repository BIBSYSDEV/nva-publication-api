package no.unit.nva.publication.publishingrequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(PublishingRequestDto.TYPE)
public class PublishingRequestDto extends TicketDto {
    
    public static final String TYPE = "PublishingRequest";
    
    public static final String STATUS_FIELD = "status";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String VERSION_FIELD = "version";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String PUBLICATION_ID_FIELD = "publicationId";
    public static final String ID_FIELD = "id";
    
    @JsonProperty(STATUS_FIELD)
    private final TicketStatus status;
    @JsonProperty(CREATED_DATE_FIELD)
    private final Instant createdDate;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private final Instant modifiedDate;
    @JsonProperty(VERSION_FIELD)
    private final UUID version;
    @JsonProperty(IDENTIFIER_FIELD)
    private final SortableIdentifier identifier;
    @JsonProperty(PUBLICATION_ID_FIELD)
    private final URI publicationId;
    @JsonProperty(ID_FIELD)
    private final URI id;
    
    @JsonCreator
    public PublishingRequestDto(@JsonProperty(STATUS_FIELD) TicketStatus status,
                                @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                                @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                                @JsonProperty(VERSION_FIELD) UUID version,
                                @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                                @JsonProperty(PUBLICATION_ID_FIELD) URI publicationId,
                                @JsonProperty(ID_FIELD) URI id) {
        super();
        this.status = status;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.version = version;
        this.identifier = identifier;
        this.publicationId = publicationId;
        this.id = id;
    }
    
    public static TicketDto empty() {
        return new PublishingRequestDto(null, null, null, null, null, null, null);
    }
    
    public TicketStatus getStatus() {
        return status;
    }
    
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    public Instant getModifiedDate() {
        return modifiedDate;
    }
    
    public UUID getVersion() {
        return version;
    }
    
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    public URI getPublicationId() {
        return publicationId;
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
        ticket.setVersion(getVersion());
        ticket.setIdentifier(getIdentifier());
        ticket.setResourceIdentifier(extractResourceIdentifier(getPublicationId()));
        return ticket;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getStatus(), getCreatedDate(), getModifiedDate(), getVersion(), getIdentifier(),
            getPublicationId(), id);
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
               && Objects.equals(getVersion(), that.getVersion())
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getPublicationId(), that.getPublicationId())
               && Objects.equals(id, that.id);
    }
}
