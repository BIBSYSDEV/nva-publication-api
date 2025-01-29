package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.associatedartifacts.file.FileResponse;
import no.unit.nva.model.associatedartifacts.file.HiddenFile;
import no.unit.nva.model.associatedartifacts.file.InternalFile;
import no.unit.nva.model.associatedartifacts.file.OpenFile;
import no.unit.nva.model.associatedartifacts.file.PendingInternalFile;
import no.unit.nva.model.associatedartifacts.file.PendingOpenFile;
import no.unit.nva.model.associatedartifacts.file.RejectedFile;
import no.unit.nva.model.associatedartifacts.file.UploadedFile;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(names ={
        PendingOpenFile.TYPE, OpenFile.TYPE, InternalFile.TYPE,
        PendingInternalFile.TYPE, HiddenFile.TYPE, RejectedFile.TYPE,
        UploadedFile.TYPE
    }, value = FileResponse.class),
    @JsonSubTypes.Type(name = AssociatedLink.TYPE_NAME, value = AssociatedLink.class),
    @JsonSubTypes.Type(name = NullAssociatedArtifact.TYPE_NAME, value = NullAssociatedArtifact.class)
})
public interface AssociatedArtifactResponse {
    String getType();
}
