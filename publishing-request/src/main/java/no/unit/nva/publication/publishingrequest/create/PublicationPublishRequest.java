package no.unit.nva.publication.publishingrequest.create;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class PublicationPublishRequest {

    @JsonCreator
    public PublicationPublishRequest() {
    }

}
