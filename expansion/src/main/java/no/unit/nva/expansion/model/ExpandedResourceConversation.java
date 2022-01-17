package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Set;
import no.unit.nva.expansion.ResourceExpansionService;
import no.unit.nva.expansion.WithOrganizationScope;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.publication.service.impl.ResourceConversation;
import no.unit.nva.publication.storage.model.Message;
import nva.commons.apigateway.exceptions.NotFoundException;

@JsonTypeName(ExpandedResourceConversation.TYPE)
public class ExpandedResourceConversation extends ResourceConversation
    implements WithOrganizationScope, ExpandedDataEntry {

    public static final String TYPE = "PublicationConversation";

    @JsonProperty("publicationIdentifier")
    private SortableIdentifier publicationIdentifier;
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

    private static ExpandedResourceConversation fromResourceConversation(ResourceConversation resourceConversation) {
        ExpandedResourceConversation expanded = new ExpandedResourceConversation();
        expanded.setPublicationSummary(resourceConversation.getPublicationSummary());
        expanded.setMessageCollections(resourceConversation.getMessageCollections());
        expanded.setOldestMessage(resourceConversation.getOldestMessage());
        expanded.setPublicationIdentifier(resourceConversation.getPublicationSummary().getPublicationIdentifier());
        return expanded;
    }

    @Override
    public Set<URI> getOrganizationIds() {
        return organizationIds;
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
}
