package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.contexttypes.PublishingHouse;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(name = "Corporation", value = Corporation.class),
  @JsonSubTypes.Type(name = "PublishingHouse", value = PublishingHouse.class),
  @JsonSubTypes.Type(name = "Identity", value = Identity.class)
})
public interface Agent {}
