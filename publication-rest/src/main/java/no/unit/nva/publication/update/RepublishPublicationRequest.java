package no.unit.nva.publication.update;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName(RepublishPublicationRequest.TYPE)
public class RepublishPublicationRequest implements PublicationRequest {

    public static final String TYPE = "RepublishPublicationRequest";
}
