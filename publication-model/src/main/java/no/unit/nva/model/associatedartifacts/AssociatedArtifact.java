package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.associatedartifacts.file.File;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(File.class),
    @JsonSubTypes.Type(name = AssociatedLink.TYPE_NAME, value = AssociatedLink.class),
    @JsonSubTypes.Type(name = NullAssociatedArtifact.TYPE_NAME, value = NullAssociatedArtifact.class)
})
public interface AssociatedArtifact {
    Set<Class<? extends AssociatedArtifact>> PUBLIC_ARTIFACT_TYPES = Set.of(OpenFile.class,
                                                                              PendingOpenFile.class,
                                                                              AssociatedLink.class);
    static Set<String> getPublicArtifactTypeNames() {
        return PUBLIC_ARTIFACT_TYPES.stream()
                   .map(Class::getSimpleName)
                   .collect(Collectors.toSet());
    }

    String getType();
    AssociatedArtifactResponse toDto();
}
