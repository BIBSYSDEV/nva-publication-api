package no.unit.nva.schemaorg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.expansion.model.ExpandedResource;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

public class SchemaOrgDocument {

    public static final String TYPE_FIELD = "type";
    @JsonProperty(TYPE_FIELD)
    private final SchemaOrgType type;

    @JsonCreator
    public SchemaOrgDocument(@JsonProperty(TYPE_FIELD) SchemaOrgType type) {
        this.type = type;
    }

    public static SchemaOrgDocument fromExpandedResource(ExpandedResource resource) {
        return new SchemaOrgDocument(SchemaOrgType.SCHOLARLY_ARTICLE);
    }

    public SchemaOrgType getType() {
        return type;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SchemaOrgDocument that)) {
            return false;
        }
        return getType() == that.getType();
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getType());
    }
}
