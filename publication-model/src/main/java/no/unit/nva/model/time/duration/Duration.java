package no.unit.nva.model.time.duration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(name = "DefinedDuration", value = DefinedDuration.class),
    @JsonSubTypes.Type(name = "UndefinedDuration", value = UndefinedDuration.class),
    @JsonSubTypes.Type(name = "NullDuration", value = NullDuration.class),
})
@Schema(oneOf = {DefinedDuration.class, UndefinedDuration.class, NullDuration.class})
public interface Duration {

}
