package no.unit.nva.model.time.duration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = "DefinedDuration", value = DefinedDuration.class),
    @JsonSubTypes.Type(name = "UndefinedDuration", value = UndefinedDuration.class),
    @JsonSubTypes.Type(name = "NullDuration", value = NullDuration.class),})
public interface Duration {

}
