package no.unit.nva.model.associatedartifacts;

import static no.unit.nva.model.associatedartifacts.AssociatedLink.DESCRIPTION_FIELD;
import static no.unit.nva.model.associatedartifacts.AssociatedLink.ID_FIELD;
import static no.unit.nva.model.associatedartifacts.AssociatedLink.NAME_FIELD;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

public record AssociatedLinkDto(@JsonProperty(ID_FIELD) URI id,
                                @JsonProperty(NAME_FIELD) String name,
                                @JsonProperty(DESCRIPTION_FIELD) String description) implements AssociatedArtifactDto {

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
