package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Organization", value = Organization.class),
    @JsonSubTypes.Type(name = "UnconfirmedOrganization", value = UnconfirmedOrganization.class)
})
public class Corporation implements Agent {

}
