package no.unit.nva.model.pages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import no.unit.nva.model.time.Period;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Range", value = Range.class),
    @JsonSubTypes.Type(name = "MonographPages", value = MonographPages.class),
    @JsonSubTypes.Type(name = "Period", value = Period.class)
})
@Schema(oneOf = {Range.class, MonographPages.class, Period.class})
public interface Pages {
    // A marker pattern interface, it may be useful later, at the moment it remains empty.
}
