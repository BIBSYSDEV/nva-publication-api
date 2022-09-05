package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import no.unit.nva.publication.model.PublicationSummary;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = ExpandedDoiRequest.TYPE, value = ExpandedDoiRequest.class),
    @JsonSubTypes.Type(name = ExpandedResourceConversation.TYPE, value = ExpandedResourceConversation.class),
    @JsonSubTypes.Type(name = ExpandedPublishingRequest.TYPE, value = ExpandedPublishingRequest.class),
})
public interface ExpandedTicket extends ExpandedDataEntry {
    
    String PUBLICATION_FIELD = "publication";
    String ORGANIZATION_IDS_FIELD = "organizationIds";
    String CREATED_DATE_FIELD = "createdDate";
    String MODIFIED_DATE_FIELD = "modifiedDate";
    
    @JsonProperty(PUBLICATION_FIELD)
    PublicationSummary getPublicationSummary();
    
    @JsonProperty(ORGANIZATION_IDS_FIELD)
    Set<URI> getOrganizationIds();
    
    @JsonAlias("date")
    @JsonProperty(CREATED_DATE_FIELD)
    Instant getCreatedDate();
    
    @JsonProperty(MODIFIED_DATE_FIELD)
    Instant getModifiedDate();
}
