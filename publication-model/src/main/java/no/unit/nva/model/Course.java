package no.unit.nva.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "UnconfirmedCourse", value = UnconfirmedCourse.class)
})
@Schema(oneOf = {UnconfirmedCourse.class})
public interface Course {

}
