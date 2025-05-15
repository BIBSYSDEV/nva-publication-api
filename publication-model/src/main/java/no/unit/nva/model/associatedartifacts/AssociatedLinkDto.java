package no.unit.nva.model.associatedartifacts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public record AssociatedLinkDto(URI id,
                                String name,
                                String description, RelationType relation) implements AssociatedArtifactDto {

    private static final String TYPE_NAME_FIELD = "type";

    @Override
    public String getArtifactType() {
        return AssociatedLink.TYPE_NAME;
    }

    @JsonProperty(TYPE_NAME_FIELD)
    public String type() {
        return AssociatedLink.TYPE_NAME;
    }
}
