package no.unit.nva.model.instancetypes.degree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "ConfirmedDocument", value = ConfirmedDocument.class),
    @JsonSubTypes.Type(name = "UnconfirmedDocument", value = UnconfirmedDocument.class)
})
public interface RelatedDocument {

}
