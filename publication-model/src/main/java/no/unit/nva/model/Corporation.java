package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Organization", value = Organization.class),
    @JsonSubTypes.Type(name = "UnconfirmedOrganization", value = UnconfirmedOrganization.class)
})
@Schema(oneOf = {Organization.class, UnconfirmedOrganization.class})
public class Corporation implements Agent {

}
