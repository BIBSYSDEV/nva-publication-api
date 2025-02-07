package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.associatedartifacts.file.FileResponse;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingInternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.RejectedFile;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;

@JsonSerialize
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = PendingOpenFile.TYPE, value = FileResponse.class),
    @JsonSubTypes.Type(name = OpenFile.TYPE, value = FileResponse.class),
    @JsonSubTypes.Type(name = InternalFile.TYPE, value = FileResponse.class),
    @JsonSubTypes.Type(name = PendingInternalFile.TYPE, value = FileResponse.class),
    @JsonSubTypes.Type(name = HiddenFile.TYPE, value = FileResponse.class),
    @JsonSubTypes.Type(name = RejectedFile.TYPE, value = FileResponse.class),
    @JsonSubTypes.Type(name = UploadedFile.TYPE, value = FileResponse.class),
    @JsonSubTypes.Type(name = AssociatedLink.TYPE_NAME, value = AssociatedLink.class),
    @JsonSubTypes.Type(name = NullAssociatedArtifact.TYPE_NAME, value = NullAssociatedArtifact.class)
})
public interface AssociatedArtifactResponse {
    @JsonIgnore
    String getArtifactType();

    @JsonCreator
    static AssociatedArtifactResponse create(JsonNode node) throws IOException {
        var mapper = JsonUtils.dtoObjectMapper;
        var type = node.get("type").asText();
        return switch (type) {
            case "PendingOpenFile", "OpenFile", "InternalFile", "PendingInternalFile", "HiddenFile", "RejectedFile",
                 "UploadedFile" -> mapper.treeToValue(node, FileResponse.class);
            case "AssociatedLink" -> mapper.treeToValue(node, AssociatedLink.class);
            case "NullAssociatedArtifact" -> mapper.treeToValue(node, NullAssociatedArtifact.class);
            default -> throw new IOException("Unknown type: " + type);
        };
    }
}
