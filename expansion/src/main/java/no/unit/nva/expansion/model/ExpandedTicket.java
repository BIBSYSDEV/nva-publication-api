package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.publication.model.PublicationSummary;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = ExpandedDoiRequest.TYPE, value = ExpandedDoiRequest.class),
    @JsonSubTypes.Type(name = ExpandedResourceConversation.TYPE, value = ExpandedResourceConversation.class),
    @JsonSubTypes.Type(name = ExpandedPublishingRequest.TYPE, value = ExpandedPublishingRequest.class),
})
public interface ExpandedTicket extends ExpandedDataEntry {
    
    String PUBLICATION_FIELD = "publication";
    
    @JsonProperty(PUBLICATION_FIELD)
    PublicationSummary getPublicationSummary();
}
