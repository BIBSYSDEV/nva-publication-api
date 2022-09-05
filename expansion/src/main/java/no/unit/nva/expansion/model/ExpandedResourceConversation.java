package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.WithOrganizationScope;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.model.ResourceConversation;
import no.unit.nva.publication.model.business.Message;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.JacocoGenerated;

@JsonTypeName(ExpandedResourceConversation.TYPE)
public class ExpandedResourceConversation extends ResourceConversation
    implements WithOrganizationScope, ExpandedTicket {
    
    public static final String TYPE = "PublicationConversation";
    
    @JsonProperty("publicationIdentifier")
    private SortableIdentifier publicationIdentifier;
    @JsonProperty(ORGANIZATION_IDS_FIELD)
    private Set<URI> organizationIds;
    
    public ExpandedResourceConversation() {
        super();
    }
    
    public static ExpandedResourceConversation create(ResourceConversation resourceConversation,
                                                      Message message,
                                                      ResourceExpansionService resourceExpansionService)
        throws NotFoundException {
        ExpandedResourceConversation expandedResourceConversation =
            ExpandedResourceConversation.fromResourceConversation(resourceConversation);
        Set<URI> organizationIds = resourceExpansionService.getOrganizationIds(message);
        expandedResourceConversation.setOrganizationIds(organizationIds);
        return expandedResourceConversation;
    }
    
    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(super.hashCode(), getPublicationIdentifier(), getOrganizationIds());
    }
    
    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExpandedResourceConversation)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ExpandedResourceConversation that = (ExpandedResourceConversation) o;
        return Objects.equals(getPublicationIdentifier(), that.getPublicationIdentifier())
               && Objects.equals(getOrganizationIds(), that.getOrganizationIds());
    }
    
    @Override
    public Set<URI> getOrganizationIds() {
        return organizationIds;
    }
    
    @Override
    public Instant getCreatedDate() {
        return null;
    }
    
    @Override
    public Instant getModifiedDate() {
        return null;
    }
    
    @Override
    public void setOrganizationIds(Set<URI> organizationIds) {
        this.organizationIds = organizationIds;
    }
    
    @Override
    public SortableIdentifier identifyExpandedEntry() {
        return getPublicationIdentifier();
    }
    
    public SortableIdentifier getPublicationIdentifier() {
        return publicationIdentifier;
    }
    
    public void setPublicationIdentifier(SortableIdentifier publicationIdentifier) {
        this.publicationIdentifier = publicationIdentifier;
    }
    
    private static ExpandedResourceConversation fromResourceConversation(ResourceConversation resourceConversation) {
        ExpandedResourceConversation expanded = new ExpandedResourceConversation();
        expanded.setPublicationSummary(resourceConversation.getPublicationSummary());
        expanded.setMessageCollections(resourceConversation.getMessageCollections());
        expanded.setOldestMessage(resourceConversation.getOldestMessage());
        expanded.setPublicationIdentifier(resourceConversation.getPublicationSummary().getPublicationIdentifier());
        return expanded;
    }
}
