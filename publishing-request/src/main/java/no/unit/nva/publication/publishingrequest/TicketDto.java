package no.unit.nva.publication.publishingrequest;

import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.MessageDto;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(DoiRequestDto.class),
    @JsonSubTypes.Type(PublishingRequestDto.class),
    @JsonSubTypes.Type(GeneralSupportRequestDto.class)
})
public abstract class TicketDto implements JsonSerializable {
    
    public static final String MESSAGES_FIELD = "messages";
    
    public static TicketDto fromTicket(TicketEntry ticket) {
        return fromTicket(ticket, Collections.emptyList());
    }
    
    public static TicketDto fromTicket(TicketEntry ticket, Collection<Message> messages) {
        return create(ticket, messages);
    }
    
    public static TicketDto create(TicketEntry ticket, Collection<Message> messages) {
        var messageDtos = messages.stream()
                              .map(MessageDto::fromMessage)
                              .collect(Collectors.toList());
        return TicketDto.builder()
                   .withCreatedDate(ticket.getCreatedDate())
                   .withStatus(ticket.getStatus())
                   .withModifiedDate(ticket.getModifiedDate())
                   .withVersion(ticket.getVersion())
                   .withIdentifier(ticket.getIdentifier())
                   .withPublicationId(createPublicationId(ticket.getResourceIdentifier()))
                   .withId(createTicketId(ticket))
                   .withMessages(messageDtos)
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
    
    public abstract TicketStatus getStatus();
    
    @Override
    public String toString() {
        return toJsonString();
    }
    
    @JsonProperty(MESSAGES_FIELD)
    public abstract List<MessageDto> getMessages();
    
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
        private List<MessageDto> messages;
        
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
        
        public Builder withMessages(List<MessageDto> messages) {
            this.messages = messages;
            return this;
        }
        
        public TicketDto build(Class<? extends TicketEntry> ticketType) {
            if (DoiRequest.class.equals(ticketType)) {
                return createDoiRequestDto();
            } else if (PublishingRequestCase.class.equals(ticketType)) {
                return createPublishingRequestDto();
            } else if (GeneralSupportRequestDto.class.equals(ticketType)) {
                return new GeneralSupportRequestDto();
            }
            throw new RuntimeException("Unsupported type");
        }
        
        private PublishingRequestDto createPublishingRequestDto() {
            return new PublishingRequestDto(status,
                createdDate,
                modifiedDate,
                version,
                identifier,
                publicationId,
                id,
                messages);
        }
        
        private DoiRequestDto createDoiRequestDto() {
            return new DoiRequestDto(status,
                createdDate,
                modifiedDate,
                version,
                identifier,
                publicationId,
                id,
                messages);
        }
    }
}
