package no.unit.nva.schemaorg.document;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import nva.commons.core.JacocoGenerated;

import java.net.URI;
import java.util.Objects;

public class FramedSchemaOrgDocument extends JsonLdBase {
    public static final String NAME_FIELD = "name";
    public static final String CREATOR_FIELD = "creator";
    public static final String PROVIDER_FIELD = "provider";
    @JsonProperty(CREATOR_FIELD)
    private final PersonI creator;
    @JsonProperty(PROVIDER_FIELD)
    private final Organization provider;
    @JsonProperty(NAME_FIELD)
    private final String name;

    @JsonCreator
    public FramedSchemaOrgDocument(@JsonProperty(CONTEXT_FIELD) URI context,
                                   @JsonProperty(ID_FIELD) URI id,
                                   @JsonProperty(TYPE_FIELD) Object type,
                                   @JsonProperty(NAME_FIELD) String name,
                                   @JsonProperty(CREATOR_FIELD) PersonI creator,
                                   @JsonProperty(PROVIDER_FIELD) Organization provider) {
        super(context, id, type);
        this.creator = creator;
        this.name = name;
        this.provider = provider;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FramedSchemaOrgDocument)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        FramedSchemaOrgDocument that = (FramedSchemaOrgDocument) o;
        return Objects.equals(creator, that.creator)
                && Objects.equals(provider, that.provider)
                && Objects.equals(name, that.name);
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(super.hashCode(), creator, provider, name);
    }
}
