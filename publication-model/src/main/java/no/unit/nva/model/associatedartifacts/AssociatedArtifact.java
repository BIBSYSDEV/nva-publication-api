package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.commons.json.JsonUtils;
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
                                                                            AssociatedLink.class,
                                                                            NullAssociatedArtifact.class);
    static Set<String> getPublicArtifactTypeNames() {
        return PUBLIC_ARTIFACT_TYPES.stream()
                   .map(Class::getSimpleName)
                   .collect(Collectors.toSet());
    }

    @JsonIgnore
    String getArtifactType();

    @JsonCreator
    static AssociatedArtifact create(JsonNode node) throws IOException {
        var mapper = JsonUtils.dtoObjectMapper;
        var type = node.get("type").asText();
        return switch (type) {
            case "PendingOpenFile", "OpenFile", "InternalFile", "PendingInternalFile", "HiddenFile", "RejectedFile",
                 "UploadedFile" -> mapper.treeToValue(node, File.class);
            case "AssociatedLink" -> mapper.treeToValue(node, AssociatedLink.class);
            case "NullAssociatedArtifact" -> mapper.treeToValue(node, NullAssociatedArtifact.class);
            default -> throw new IOException("Unknown type: " + type);
        };
    }

    AssociatedArtifactDto toDto();
}
