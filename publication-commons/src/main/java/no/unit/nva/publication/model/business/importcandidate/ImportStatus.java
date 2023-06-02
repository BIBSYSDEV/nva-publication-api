package no.unit.nva.publication.model.business.importcandidate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "Imported", value = Imported.class),
        @JsonSubTypes.Type(name = "NotApplicable", value = NotApplicable.class),
        @JsonSubTypes.Type(name = "NotImported", value = NotImported.class),
})
public interface ImportStatus {

}
