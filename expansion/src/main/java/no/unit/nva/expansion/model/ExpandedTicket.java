package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.util.List;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.*;
import no.unit.nva.publication.service.impl.ResourceService;
import no.unit.nva.publication.service.impl.TicketService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.paths.UriWrapper;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = ExpandedDoiRequest.TYPE, value = ExpandedDoiRequest.class),
    @JsonSubTypes.Type(name = ExpandedPublishingRequest.TYPE, value = ExpandedPublishingRequest.class),
    @JsonSubTypes.Type(name = ExpandedGeneralSupportRequest.TYPE, value = ExpandedGeneralSupportRequest.class)
})
public abstract class ExpandedTicket implements ExpandedDataEntry {
    
    public static final String PUBLICATION_FIELD = "publication";
    public static final String ORGANIZATION_IDS_FIELD = "organizationIds";
    public static final String ID_FIELD = "id";
    public static final String VIEWED_BY_FIELD = "viewedBy";
    private static final String MESSAGES_FIELD = "messages";
    
    @JsonProperty(ID_FIELD)
    private URI id;
    @JsonProperty(MESSAGES_FIELD)
    private List<Message> messages;
    @JsonProperty(VIEWED_BY_FIELD)
    private Set<User> viewedBy;
    @JsonProperty(PUBLICATION_FIELD)
    private PublicationSummary publication;

    public static ExpandedDataEntry create(TicketEntry ticketEntry,
                                           ResourceService resourceService,
                                           ResourceExpansionService expansionService,
                                           TicketService ticketService) throws NotFoundException {
        
        if (ticketEntry instanceof DoiRequest) {
            return ExpandedDoiRequest.createEntry((DoiRequest) ticketEntry,
                expansionService,
                resourceService,
                ticketService);
        }
        if (ticketEntry instanceof PublishingRequestCase) {
            return ExpandedPublishingRequest.createEntry(
                (PublishingRequestCase) ticketEntry,
                resourceService,
                expansionService,
                ticketService);
        }
        if (ticketEntry instanceof GeneralSupportRequest) {
            return ExpandedGeneralSupportRequest.createEntry(
                (GeneralSupportRequest) ticketEntry,
                resourceService,
                expansionService,
                ticketService
            );
        }
        throw new UnsupportedOperationException();
    }
    
    public final Set<User> getViewedBy() {
        return viewedBy;
    }
    
    public final void setViewedBy(Set<User> viewedBy) {
        this.viewedBy = viewedBy;
    }
    
    @JsonProperty(PUBLICATION_FIELD)
    public final PublicationSummary getPublication() {
        return this.publication;
    }
    
    public final void setPublication(PublicationSummary publication) {
        this.publication = publication;
    }
    
    @JsonProperty(ORGANIZATION_IDS_FIELD)
    public abstract Set<URI> getOrganizationIds();
    
    public abstract TicketEntry toTicketEntry();
    
    @JsonProperty(ID_FIELD)
    public final URI getId() {
        return this.id;
    }
    
    public final void setId(URI id) {
        this.id = id;
    }
    
    public abstract TicketStatus getStatus();
    
    public final List<Message> getMessages() {
        return this.messages;
    }
    
    public final void setMessages(List<Message> messages) {
        this.messages = messages;
    }
    
    protected static URI generateId(URI publicationId, SortableIdentifier identifier) {
        return UriWrapper.fromUri(publicationId)
                   .addChild(PublicationServiceConfig.TICKET_PATH)
                   .addChild(identifier.toString())
                   .getUri();
    }
    
    protected static SortableIdentifier extractIdentifier(URI id) {
        return new SortableIdentifier(UriWrapper.fromUri(id).getLastPathElement());
    }
}
