package no.unit.nva.publication.model.business.logentry;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = LogOrganization.TYPE, value = LogOrganization.class),
    @JsonSubTypes.Type(name = LogUser.TYPE, value = LogUser.class)})
public interface LogAgent {

}
