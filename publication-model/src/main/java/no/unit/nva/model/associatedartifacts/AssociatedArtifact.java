package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.associatedartifacts.file.File;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(File.class),
    @JsonSubTypes.Type(name = AssociatedLink.TYPE_NAME, value = AssociatedLink.class),
    @JsonSubTypes.Type(name = NullAssociatedArtifact.TYPE_NAME, value = NullAssociatedArtifact.class)
})
public interface AssociatedArtifact {

}
