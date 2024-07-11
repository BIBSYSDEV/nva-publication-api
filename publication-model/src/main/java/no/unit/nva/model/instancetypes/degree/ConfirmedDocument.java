package no.unit.nva.model.instancetypes.degree;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public record ConfirmedDocument(URI identifier) implements RelatedDocument{
}
