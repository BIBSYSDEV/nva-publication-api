package no.unit.nva.publication.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(UpdatePublicationRequest.class),
    @JsonSubTypes.Type(UnpublishPublicationRequest.class),
    @JsonSubTypes.Type(DeletePublicationRequest.class),
    @JsonSubTypes.Type(RepublishPublicationRequest.class),
    @JsonSubTypes.Type(PartialUpdatePublicationRequest.class)
})
public interface PublicationRequest {

}
