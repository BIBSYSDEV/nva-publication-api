package no.unit.nva.publication.ticket;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.PublicationServiceConfig.API_HOST;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.Message;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.TicketStatus;
import no.unit.nva.publication.model.business.User;
import no.unit.nva.publication.model.business.ViewedBy;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(DoiRequestDto.class),
    @JsonSubTypes.Type(PublishingRequestDto.class),
    @JsonSubTypes.Type(GeneralSupportRequestDto.class)
})
public abstract class TicketDto implements JsonSerializable {
    
    public static final String STATUS_FIELD = "status";
    public static final String MESSAGES_FIELD = "messages";
    public static final String VIEWED_BY = "viewedBy";
    public static final String PUBLICATION_FIELD = "publication";
    public static final String ASSIGNEE_FIELD = "assignee";
    @JsonProperty(PUBLICATION_FIELD)
    private final PublicationSummary publicationSummary;
    @JsonProperty(STATUS_FIELD)
    private final TicketStatus status;
    @JsonProperty(VIEWED_BY)
    private final Set<User> viewedBy;
    @JsonProperty(MESSAGES_FIELD)
    private final List<MessageDto> messages;
    @JsonProperty(ASSIGNEE_FIELD)
    private final User assignee;
    
    protected TicketDto(TicketStatus status,
                        List<MessageDto> messages,
                        Set<User> viewedBy,
                        PublicationSummary publicationSummary,
                        User assignee) {
        this.status = status;
        this.messages = messages;
        this.viewedBy = new ViewedBy(viewedBy);
        this.publicationSummary = publicationSummary;
        this.assignee = assignee;
    }
    
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
                   .withIdentifier(ticket.getIdentifier())
                   .withId(createTicketId(ticket))
                   .withPublicationSummary(createPublicationSummary(ticket))
                   .withMessages(messageDtos)
                   .withViewedBy(ticket.getViewedBy())
                   .withAssignee(ticket.getAssignee())
                   .build(ticket.getClass());
    }
    
    public static Builder builder() {
        return new TicketDto.Builder();
    }
    
    public static URI createTicketId(TicketEntry ticket) {
        return UriWrapper.fromUri(createPublicationId(ticket.extractPublicationIdentifier()))
                   .addChild(PublicationServiceConfig.TICKET_PATH)
                   .addChild(ticket.getIdentifier().toString())
                   .getUri();
    }
    
    public PublicationSummary getPublicationSummary() {
        return publicationSummary;
    }

    public User getAssignee() {
        return assignee;
    }

    public abstract Class<? extends TicketEntry> ticketType();
    
    public abstract TicketEntry toTicket();
    
    public final TicketStatus getStatus() {
        return status;
    }
    
    public final List<MessageDto> getMessages() {
        return nonNull(messages) ? messages : Collections.emptyList();
    }
    
    public Set<User> getViewedBy() {
        return viewedBy;
    }
    
    private static PublicationSummary createPublicationSummary(TicketEntry ticket) {
        return PublicationSummary.create(ticket.getPublicationDetails());
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
        private SortableIdentifier identifier;
        private URI id;
        private List<MessageDto> messages;
        private ViewedBy viewedBy;
        private PublicationSummary publicationSummary;
        private User assignee;
        
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
        
        public Builder withIdentifier(SortableIdentifier identifier) {
            this.identifier = identifier;
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

        public Builder withAssignee(User assignee) {
            this.assignee = assignee;
            return this;
        }
        
        public Builder withPublicationSummary(PublicationSummary publicationSummary) {
            this.publicationSummary = publicationSummary;
            return this;
        }
        
        public TicketDto build(Class<? extends TicketEntry> ticketType) {
            
            if (DoiRequest.class.equals(ticketType)) {
                return createDoiRequestDto();
            } else if (PublishingRequestCase.class.equals(ticketType)) {
                return createPublishingRequestDto();
            } else if (GeneralSupportRequest.class.equals(ticketType)) {
                return new GeneralSupportRequestDto(status,
                    createdDate,
                    modifiedDate,
                    identifier,
                    publicationSummary,
                    id,
                    messages,
                    viewedBy,
                    assignee);
            }
            throw new RuntimeException("Unsupported type");
        }
        
        public Builder withViewedBy(Set<User> viewedBy) {
            this.viewedBy = new ViewedBy(viewedBy);
            return this;
        }
        
        private PublishingRequestDto createPublishingRequestDto() {
            return new PublishingRequestDto(status,
                createdDate,
                modifiedDate,
                identifier,
                publicationSummary,
                id,
                messages,
                viewedBy,
                assignee);
        }
        
        private DoiRequestDto createDoiRequestDto() {
            return new DoiRequestDto(status,
                createdDate,
                modifiedDate,
                identifier,
                publicationSummary,
                id,
                messages,
                viewedBy,
                assignee);
        }
    }
}
