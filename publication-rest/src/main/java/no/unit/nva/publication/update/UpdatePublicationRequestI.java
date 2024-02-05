package no.unit.nva.publication.update;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(UpdatePublicationMetadataRequest.class),
    @JsonSubTypes.Type(UnpublishPublicationRequest.class),
    @JsonSubTypes.Type(DeletePublicationRequest.class)
})
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class UpdatePublicationRequestI {

}
