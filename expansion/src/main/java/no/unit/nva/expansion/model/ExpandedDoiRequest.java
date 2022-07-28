package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.WithOrganizationScope;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.MessageCollection;
import no.unit.nva.publication.model.PublicationSummary;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.model.business.DoiRequest;
import no.unit.nva.publication.model.business.DoiRequestStatus;
import no.unit.nva.publication.model.business.MessageType;
import no.unit.nva.publication.model.business.UserInstance;
import no.unit.nva.publication.service.impl.MessageService;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(ExpandedDoiRequest.TYPE)
@SuppressWarnings("PMD.TooManyFields")
public final class ExpandedDoiRequest implements WithOrganizationScope, ExpandedTicket {
    
    public static final String TYPE = "DoiRequest";
    
    @JsonProperty
    private SortableIdentifier identifier;
    @JsonProperty()
    private DoiRequestStatus status;
    @JsonProperty
    private Instant modifiedDate;
    @JsonProperty
    @JsonAlias("date")
    private Instant createdDate;
    @JsonProperty
    private URI customerId;
    @JsonProperty
    private String owner;
    
    private PublicationSummary publicationSummary;
    @JsonProperty("doi")
    private URI doi;
    @JsonProperty(ORGANIZATION_IDS_FIELD)
    private Set<URI> organizationIds;
    @JsonProperty("messages")
    private MessageCollection doiRequestMessages;
    
    public static ExpandedDoiRequest create(DoiRequest doiRequest,
                                            ResourceExpansionService expansionService,
                                            MessageService messageService)
        throws NotFoundException {
        var expandedDoiRequest = ExpandedDoiRequest.fromDoiRequest(doiRequest);
        expandedDoiRequest.setDoiRequestMessages(fetchDoiRequestMessagesForResource(messageService, doiRequest));
        expandedDoiRequest.setOrganizationIds(fetchOrganizationIdsForViewingScope(doiRequest, expansionService));
        return expandedDoiRequest;
    }
    
    public MessageCollection getDoiRequestMessages() {
        return doiRequestMessages;
    }
    
    public void setDoiRequestMessages(MessageCollection doiRequestMessages) {
        this.doiRequestMessages = doiRequestMessages;
    }
    
    @JacocoGenerated
    public SortableIdentifier getIdentifier() {
        return identifier;
    }
    
    @JacocoGenerated
    public void setIdentifier(SortableIdentifier identifier) {
        this.identifier = identifier;
    }
    
    @JacocoGenerated
    public DoiRequestStatus getStatus() {
        return status;
    }
    
    @JacocoGenerated
    public void setStatus(DoiRequestStatus status) {
        this.status = status;
    }
    
    @JacocoGenerated
    public Instant getModifiedDate() {
        return modifiedDate;
    }
    
    @JacocoGenerated
    public void setModifiedDate(Instant modifiedDate) {
        this.modifiedDate = modifiedDate;
    }
    
    @JacocoGenerated
    public Instant getCreatedDate() {
        return createdDate;
    }
    
    @JacocoGenerated
    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }
    
    @JacocoGenerated
    public URI getCustomerId() {
        return customerId;
    }
    
    @JacocoGenerated
    public void setCustomerId(URI customerId) {
        this.customerId = customerId;
    }
    
    @JacocoGenerated
    public String getOwner() {
        return owner;
    }
    
    @JacocoGenerated
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    @Override
    @JacocoGenerated
    public PublicationSummary getPublicationSummary() {
        return publicationSummary;
    }
    
    @JacocoGenerated
    public void setPublicationSummary(PublicationSummary publicationSummary) {
        this.publicationSummary = publicationSummary;
    }
    
    @JacocoGenerated
    public URI getDoi() {
        return doi;
    }
    
    @JacocoGenerated
    public void setDoi(URI doi) {
        this.doi = doi;
    }
    
    @JacocoGenerated
    @Override
    public Set<URI> getOrganizationIds() {
        return organizationIds;
    }
    
    @JacocoGenerated
    @Override
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }
    
    public DoiRequest toDoiRequest() {
        DoiRequest doiRequest = new DoiRequest();
        doiRequest.setDoi(this.getDoi());
        doiRequest.setContributors(this.getPublicationSummary().getContributors());
        doiRequest.setCreatedDate(this.getCreatedDate());
        doiRequest.setIdentifier(this.getIdentifier());
        doiRequest.setCustomerId(this.getCustomerId());
        doiRequest.setModifiedDate(this.getModifiedDate());
        doiRequest.setOwner(this.getOwner());
        doiRequest.setResourceIdentifier(SortableIdentifier.fromUri(this.getPublicationSummary().getPublicationId()));
        doiRequest.setResourceModifiedDate(this.getPublicationSummary().getModifiedDate());
        doiRequest.setResourcePublicationDate(this.getPublicationSummary().getPublicationDate());
        doiRequest.setResourcePublicationInstance(this.getPublicationSummary().getPublicationInstance());
        doiRequest.setResourcePublicationYear(this.getPublicationSummary().getPublicationYear());
        doiRequest.setResourceStatus(this.getPublicationSummary().getStatus());
        doiRequest.setResourceTitle(this.getPublicationSummary().getTitle());
        doiRequest.setStatus(this.getStatus());
        return doiRequest;
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return getIdentifier();
    }
    
    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getIdentifier(), getStatus(), getModifiedDate(), getCreatedDate(),
            getCustomerId(), getOwner(), getPublicationSummary(), getDoi(), getOrganizationIds());
    }
    
    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpandedDoiRequest)) {
            return false;
        }
        ExpandedDoiRequest that = (ExpandedDoiRequest) o;
        return Objects.equals(getIdentifier(), that.getIdentifier())
               && getStatus() == that.getStatus()
               && Objects.equals(getModifiedDate(), that.getModifiedDate())
               && Objects.equals(getCreatedDate(), that.getCreatedDate())
               && Objects.equals(getCustomerId(), that.getCustomerId())
               && Objects.equals(getOwner(), that.getOwner())
               && Objects.equals(getPublicationSummary(), that.getPublicationSummary())
               && Objects.equals(getDoi(), that.getDoi())
               && Objects.equals(getOrganizationIds(), that.getOrganizationIds());
    }
    
    private static Set<URI> fetchOrganizationIdsForViewingScope(DoiRequest doiRequest,
                                                                ResourceExpansionService resourceExpansionService)
        throws NotFoundException {
        return resourceExpansionService.getOrganizationIds(doiRequest);
    }
    
    private static MessageCollection fetchDoiRequestMessagesForResource(MessageService messageService,
                                                                        DoiRequest doiRequest) {
        
        UserInstance userInstance = UserInstance.fromDoiRequest(doiRequest);
        return fetchAllMessagesForResource(messageService, doiRequest, userInstance)
            .map(conversation -> conversation.getMessageCollectionOfType(MessageType.DOI_REQUEST))
            .orElse(MessageCollection.empty(MessageType.DOI_REQUEST));
    }
    
    private static Optional<ResourceConversation> fetchAllMessagesForResource(MessageService messageService,
                                                                              DoiRequest doiRequest,
                                                                              UserInstance userInstance) {
        return messageService.getMessagesForResource(userInstance, doiRequest.getResourceIdentifier());
    }
    
    // should not become public. An ExpandedDoiRequest needs an Expansion service to be complete
    private static ExpandedDoiRequest fromDoiRequest(DoiRequest doiRequest) {
        ExpandedDoiRequest request = new ExpandedDoiRequest();
        request.setDoi(doiRequest.getDoi());
        request.setPublicationSummary(PublicationSummary.create(doiRequest));
        request.setCreatedDate(doiRequest.getCreatedDate());
        request.setIdentifier(doiRequest.getIdentifier());
        request.setCustomerId(doiRequest.getCustomerId());
        request.setModifiedDate(doiRequest.getModifiedDate());
        request.setOwner(doiRequest.getOwner());
        request.setStatus(doiRequest.getStatus());
        return request;
    }
}
