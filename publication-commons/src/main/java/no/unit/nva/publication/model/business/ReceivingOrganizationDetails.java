package no.unit.nva.publication.model.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.net.URI;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("ReceivingOrganizationDetails")
public record ReceivingOrganizationDetails(@JsonProperty("topLevelOrganizationId") URI topLevelOrganizationId,
                                           @JsonProperty("subOrganizationId") URI subOrganizationId) {
}
