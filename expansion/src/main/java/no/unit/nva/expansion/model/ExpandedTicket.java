package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.PublicationServiceConfig;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.FilesApprovalThesis;
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
    @JsonSubTypes.Type(name = ExpandedGeneralSupportRequest.TYPE, value = ExpandedGeneralSupportRequest.class),
    @JsonSubTypes.Type(name = ExpandedUnpublishRequest.TYPE, value = ExpandedUnpublishRequest.class),
    @JsonSubTypes.Type(name = ExpandedFileApprovalThesis.TYPE, value = ExpandedFileApprovalThesis.class)
})
public abstract class ExpandedTicket implements ExpandedDataEntry {

    private static final String PUBLICATION_FIELD = "publication";
    private static final String ORGANIZATION_FIELD = "organization";
    private static final String ID_FIELD = "id";
    private static final String VIEWED_BY_FIELD = "viewedBy";
    private static final String ASSIGNEE_FIELD = "assignee";
    private static final String FINALIZED_BY_FIELD = "finalizedBy";
    private static final String OWNER_FIELD = "owner";
    private static final String MESSAGES_FIELD = "messages";
    protected static final String MODIFIED_DATE_FIELD = "modifiedDate";
    protected static final String CREATED_DATE_FIELD = "createdDate";
    protected static final String CUSTOMER_ID_FIELD = "customerId";
    protected static final String STATUS_FIELD = "status";

    @JsonProperty(CUSTOMER_ID_FIELD)
    private URI customerId;
    @JsonProperty(MODIFIED_DATE_FIELD)
    private Instant modifiedDate;
    @JsonProperty(CREATED_DATE_FIELD)
    private Instant createdDate;
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
    @JsonProperty(STATUS_FIELD)
    private ExpandedTicketStatus status;


    public static ExpandedDataEntry create(TicketEntry ticketEntry,
                                           ResourceService resourceService,
                                           ResourceExpansionService expansionService,
                                           TicketService ticketService)
        throws NotFoundException, JsonProcessingException {
        return switch (ticketEntry) {
            case DoiRequest doiRequest -> ExpandedDoiRequest.createEntry(doiRequest,
                                                                         expansionService,
                                                                         resourceService,
                                                                         ticketService);
            case PublishingRequestCase publishingRequestCase -> ExpandedPublishingRequest.createEntry(
                publishingRequestCase,
                resourceService,
                expansionService,
                ticketService);
            case GeneralSupportRequest generalSupportRequest -> ExpandedGeneralSupportRequest.createEntry(
                generalSupportRequest,
                resourceService,
                expansionService,
                ticketService
            );
            case UnpublishRequest unpublishRequest -> ExpandedUnpublishRequest.createEntry(
                unpublishRequest,
                resourceService,
                expansionService,
                ticketService
            );
            case FilesApprovalThesis filesApprovalThesis -> ExpandedFileApprovalThesis.createEntry(
                filesApprovalThesis,
                resourceService,
                expansionService,
                ticketService
            );
            default -> throw new UnsupportedOperationException("Unsupported ticket entry type %s"
                                                                   .formatted(ticketEntry.getClass().getSimpleName()));
        };
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

    public ExpandedTicketStatus getStatus() {
        return status;
    }

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

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public URI getCustomerId() {
        return customerId;
    }

    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }

    public void setStatus(ExpandedTicketStatus status) {
        this.status = status;
    }

    protected static URI generateId(URI publicationId, SortableIdentifier identifier) {
        return UriWrapper.fromUri(publicationId)
            .addChild(PublicationServiceConfig.TICKET_PATH)
            .addChild(identifier.toString())
            .getUri();
    }
}