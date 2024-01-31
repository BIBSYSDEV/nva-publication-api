package no.unit.nva.publication.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UpdatePublicationMetadataRequest.class),
    @JsonSubTypes.Type(value = UnpublishPublicationRequest.class),
    @JsonSubTypes.Type(value = DeletePublicationRequest.class)
})
public abstract class UpdatePublicationRequestI {

}
