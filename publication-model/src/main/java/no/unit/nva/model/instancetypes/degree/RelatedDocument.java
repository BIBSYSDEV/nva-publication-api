package no.unit.nva.model.instancetypes.degree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "ConfirmedDocument", value = ConfirmedDocument.class),
    @JsonSubTypes.Type(name = "UnconfirmedDocument", value = UnconfirmedDocument.class)
})
@Schema(oneOf = {ConfirmedDocument.class, UnconfirmedDocument.class})
public interface RelatedDocument {

}
