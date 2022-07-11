package no.unit.nva.publication.publishingrequest.create;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("PublishingRequest")
public class NewPublishingRequest {

    @JsonCreator
    public NewPublishingRequest() {
    }

}
