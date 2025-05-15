package no.unit.nva.model.associatedartifacts;


import com.fasterxml.jackson.annotation.JsonProperty;

public record NullAssociatedArtifactDto() implements AssociatedArtifactDto {

    private static final String TYPE_NAME_FIELD = "type";

    @Override
    public String getArtifactType() {
        return  NullAssociatedArtifact.TYPE_NAME;
    }

    @JsonProperty(TYPE_NAME_FIELD)
    public String type() {
        return  NullAssociatedArtifact.TYPE_NAME;
    }
}
