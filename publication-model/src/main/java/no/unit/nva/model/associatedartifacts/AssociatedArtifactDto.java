package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.file.FileDto;

public interface AssociatedArtifactDto {
    @JsonIgnore
    String getArtifactType();

    @JsonCreator
    static AssociatedArtifactDto create(JsonNode node) throws IOException {
        var mapper = JsonUtils.dtoObjectMapper;

        var type = node.get("type").asText();
        return switch (type) {
            case "PendingOpenFile", "OpenFile", "InternalFile", "PendingInternalFile", "HiddenFile", "RejectedFile",
                 "UploadedFile" -> mapper.treeToValue(node, FileDto.class);
            case "AssociatedLink" -> mapper.treeToValue(node, AssociatedLinkDto.class);
            case "NullAssociatedArtifact" -> mapper.treeToValue(node, NullAssociatedArtifactDto.class);
            default -> throw new IOException("Unknown type: " + type);
        };
    }
}
