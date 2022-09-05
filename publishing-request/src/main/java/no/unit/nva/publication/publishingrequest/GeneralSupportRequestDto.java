package no.unit.nva.publication.publishingrequest;

import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(GeneralSupportRequestDto.TYPE)
public class GeneralSupportRequestDto extends TicketDto {
    
    public static final String TYPE = "GeneralSupportCase";
    public static final String STATUS_FIELD = "status";
    public static final String CREATED_DATE_FIELD = "createdDate";
    public static final String MODIFIED_DATE_FIELD = "modifiedDate";
    public static final String VERSION_FIELD = "version";
    public static final String IDENTIFIER_FIELD = "identifier";
    public static final String PUBLICATION_ID_FIELD = "publicationId";
    public static final String ID_FIELD = "id";
    @JsonProperty(ID_FIELD)
    private final URI id;
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
    @JsonProperty(MESSAGES_FIELD)
    private final List<MessageDto> messages;
    
    public GeneralSupportRequestDto(@JsonProperty(STATUS_FIELD) TicketStatus status,
                                    @JsonProperty(CREATED_DATE_FIELD) Instant createdDate,
                                    @JsonProperty(MODIFIED_DATE_FIELD) Instant modifiedDate,
                                    @JsonProperty(VERSION_FIELD) UUID version,
                                    @JsonProperty(IDENTIFIER_FIELD) SortableIdentifier identifier,
                                    @JsonProperty(PUBLICATION_ID_FIELD) URI publicationId,
                                    @JsonProperty(ID_FIELD) URI id,
                                    @JsonProperty(MESSAGES_FIELD) List<MessageDto> messages) {
        super();
        this.status = status;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.version = version;
        this.identifier = identifier;
        this.publicationId = publicationId;
        this.id = id;
        this.messages = messages;
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
    
    public UUID getVersion() {
        return version;
    }
    
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    public URI getPublicationId() {
        return publicationId;
    }
    
    @JacocoGenerated
    @Override
    public Class<? extends TicketEntry> ticketType() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public TicketEntry toTicket() {
        var request = new GeneralSupportRequest();
        request.setIdentifier(this.getIdentifier());
        request.setVersion(this.getVersion());
        request.setStatus(this.getStatus());
        request.setResourceIdentifier(extractResourceIdentifier(this.getPublicationId()));
        request.setCreatedDate(this.getCreatedDate());
        request.setModifiedDate(this.getModifiedDate());
        return request;
    }
    
    @Override
    public TicketStatus getStatus() {
        return status;
    }
    
    @Override
    public List<MessageDto> getMessages() {
        return nonNull(messages) ? messages : Collections.emptyList();
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
               && Objects.equals(getVersion(), that.getVersion())
               && Objects.equals(getIdentifier(), that.getIdentifier())
               && Objects.equals(getPublicationId(), that.getPublicationId())
               && Objects.equals(getMessages(), that.getMessages());
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getId(), getStatus(), getCreatedDate(), getModifiedDate(), getVersion(), getIdentifier(),
            getPublicationId(), getMessages());
    }
}
