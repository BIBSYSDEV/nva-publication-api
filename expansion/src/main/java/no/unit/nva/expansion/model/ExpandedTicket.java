package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.List;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.GeneralSupportRequest;
import no.unit.nva.publication.model.business.PublishingRequestCase;
import no.unit.nva.publication.model.business.TicketEntry;
import no.unit.nva.publication.model.business.UnpublishRequest;
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
    public static final String ORGANIZATION_FIELD = "organization";
    public static final String ID_FIELD = "id";
    public static final String VIEWED_BY_FIELD = "viewedBy";
    public static final String ASSIGNEE_FIELD = "assignee";
    public static final String FINALIZED_BY_FIELD = "finalizedBy";
    public static final String OWNER_FIELD = "owner";
    private static final String MESSAGES_FIELD = "messages";
    @JsonProperty(ID_FIELD)
    private URI id;
    @JsonProperty(MESSAGES_FIELD)
    private List<ExpandedMessage> messages;
    @JsonProperty(VIEWED_BY_FIELD)
    private Set<ExpandedPerson> viewedBy;
    @JsonProperty(PUBLICATION_FIELD)
    private PublicationSummary publication;
    @JsonProperty(FINALIZED_BY_FIELD)
    private ExpandedPerson finalizedBy;
    @JsonProperty(OWNER_FIELD)
    private ExpandedPerson owner;
    @JsonProperty(ASSIGNEE_FIELD)
    private ExpandedPerson assignee;
    @JsonProperty(ORGANIZATION_FIELD)
    private ExpandedOrganization organization;

    public static ExpandedDataEntry create(TicketEntry ticketEntry,
                                           ResourceService resourceService,
                                           ResourceExpansionService expansionService,
                                           TicketService ticketService)
        throws NotFoundException, JsonProcessingException {

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
        if (ticketEntry instanceof UnpublishRequest) {
            return ExpandedUnpublishRequest.createEntry(
                (UnpublishRequest) ticketEntry,
                resourceService,
                expansionService,
                ticketService
            );
        }
        throw new UnsupportedOperationException();
    }

    public static SortableIdentifier extractIdentifier(URI id) {
        return new SortableIdentifier(UriWrapper.fromUri(id).getLastPathElement());
    }

    public ExpandedPerson getFinalizedBy() {
        return finalizedBy;
    }

    public void setFinalizedBy(ExpandedPerson finalizedBy) {
        this.finalizedBy = finalizedBy;
    }

    public final Set<ExpandedPerson> getViewedBy() {
        return viewedBy;
    }

    public final void setViewedBy(Set<ExpandedPerson> viewedBy) {
        this.viewedBy = viewedBy;
    }

    @JsonProperty(PUBLICATION_FIELD)
    public final PublicationSummary getPublication() {
        return this.publication;
    }

    public final void setPublication(PublicationSummary publication) {
        this.publication = publication;
    }

    @JsonProperty(ORGANIZATION_FIELD)
    public ExpandedOrganization getOrganization() {
        return this.organization;
    }

    public void setOrganization(ExpandedOrganization organization) {
        this.organization = organization;
    }

    @JsonProperty(ID_FIELD)
    public final URI getId() {
        return this.id;
    }

    public final void setId(URI id) {
        this.id = id;
    }

    public abstract ExpandedTicketStatus getStatus();

    public final List<ExpandedMessage> getMessages() {
        return this.messages;
    }

    public final void setMessages(List<ExpandedMessage> messages) {
        this.messages = messages;
    }

    public final ExpandedPerson getOwner() {
        return this.owner;
    }

    public final void setOwner(ExpandedPerson owner) {
        this.owner = owner;
    }

    public final ExpandedPerson getAssignee() {
        return assignee;
    }

    public final void setAssignee(ExpandedPerson assignee) {
        this.assignee = assignee;
    }

    protected static URI generateId(URI publicationId, SortableIdentifier identifier) {
        return UriWrapper.fromUri(publicationId)
            .addChild(PublicationServiceConfig.TICKET_PATH)
            .addChild(identifier.toString())
            .getUri();
    }
}