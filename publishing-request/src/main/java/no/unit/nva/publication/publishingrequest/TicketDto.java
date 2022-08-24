package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(DoiRequestDto.class),
    @JsonSubTypes.Type(PublishingRequestDto.class)
})
public abstract class TicketDto implements JsonSerializable {
    
    public static TicketDto fromTicket(TicketEntry ticket) {
        return create(ticket);
    }
    
    public static TicketDto create(TicketEntry ticket) {
        return TicketDto.builder()
                   .withCreatedDate(ticket.getCreatedDate())
                   .withStatus(ticket.getStatus())
                   .withModifiedDate(ticket.getModifiedDate())
                   .withVersion(ticket.getVersion())
                   .withIdentifier(ticket.getIdentifier())
                   .withPublicationId(createPublicationId(ticket.getResourceIdentifier()))
                   .withId(createTicketId(ticket))
                   .build(ticket.getClass());
    }
    
    public static TicketDto fromJson(String json) {
        return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, TicketDto.class)).orElseThrow();
    }
    
    public static Builder builder() {
        return new TicketDto.Builder();
    }
    
    public abstract Class<? extends TicketEntry> ticketType();
    
    public abstract TicketEntry toTicket();
    
    @Override
    public String toString() {
        return toJsonString();
    }
    
    protected SortableIdentifier extractResourceIdentifier(URI publicationId) {
        var idString = UriWrapper.fromUri(publicationId).getLastPathElement();
        return new SortableIdentifier(idString);
    }
    
    private static URI createTicketId(TicketEntry ticket) {
        return UriWrapper.fromUri(createPublicationId(ticket.getResourceIdentifier()))
                   .addChild(TicketUtils.TICKET_IDENTIFIER_PATH_PARAMETER)
                   .addChild(ticket.getIdentifier().toString())
                   .getUri();
    }
    
    private static URI createPublicationId(SortableIdentifier publicationIdentifier) {
        return UriWrapper.fromHost(API_HOST)
                   .addChild(PublicationServiceConfig.PUBLICATION_PATH)
                   .addChild(publicationIdentifier.toString())
                   .getUri();
    }
    
    public static final class Builder {
        
        private TicketStatus status;
        private Instant createdDate;
        private Instant modifiedDate;
        private UUID version;
        private SortableIdentifier identifier;
        private URI publicationId;
        private URI id;
        
        private Builder() {
        }
        
        public Builder withStatus(TicketStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder withCreatedDate(Instant createdDate) {
            this.createdDate = createdDate;
            return this;
        }
        
        public Builder withModifiedDate(Instant modifiedDate) {
            this.modifiedDate = modifiedDate;
            return this;
        }
        
        public Builder withVersion(UUID version) {
            this.version = version;
            return this;
        }
        
        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
            return this;
        }
        
        public Builder withPublicationId(URI publicationId) {
            this.publicationId = publicationId;
            return this;
        }
        
        public Builder withId(URI id) {
            this.id = id;
            return this;
        }
        
        public TicketDto build(Class<? extends TicketEntry> ticketType) {
            if (DoiRequest.class.equals(ticketType)) {
                return new DoiRequestDto(status,
                    createdDate,
                    modifiedDate,
                    version,
                    identifier,
                    publicationId,
                    id);
            } else if (PublishingRequestCase.class.equals(ticketType)) {
                return new PublishingRequestDto(status,
                    createdDate,
                    modifiedDate,
                    version,
                    identifier,
                    publicationId,
                    id);
            }
            throw new RuntimeException("Unsupported DTO type");
        }
    }
}
