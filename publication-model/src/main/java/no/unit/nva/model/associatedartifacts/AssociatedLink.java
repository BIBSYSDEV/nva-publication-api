package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName(AssociatedLink.TYPE_NAME)
public record AssociatedLink(URI id, String name, String description, RelationType relation)
    implements AssociatedArtifact {

    public static final String TYPE_NAME = "AssociatedLink";

    @Override
    public String getArtifactType() {
        return TYPE_NAME;
    }

    @Override
    public AssociatedArtifactDto toDto() {
        return new AssociatedLinkDto(id, name, description, relation);
    }
}
