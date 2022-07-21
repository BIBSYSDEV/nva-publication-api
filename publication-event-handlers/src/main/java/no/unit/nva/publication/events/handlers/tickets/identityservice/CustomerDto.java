package no.unit.nva.publication.events.handlers.tickets.identityservice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Customer")
public class CustomerDto {
    
    @JsonProperty("publicationWorkflow")
    //property does not have getter because we are never going to serialize this object
    private PublicationWorkflow publicationWorkflow;
    
    public void setPublicationWorkflow(PublicationWorkflow publicationWorkflow) {
        this.publicationWorkflow = publicationWorkflow;
    }
    
    @JsonIgnore
    public Boolean customerAllowsRegistratorsToPublishDataAndMetadata() {
        return PublicationWorkflow.REGISTRATOR_PUBLISHES_METADATA_AND_FILES.equals(publicationWorkflow);
    }
}
