package no.unit.nva.schemaorg;

import no.unit.nva.expansion.model.ExpandedResource;

public class SchemaOrgDocument {
    private final SchemaOrgType type;

    public SchemaOrgDocument(SchemaOrgType type) {
        this.type = type;
    }

    public static SchemaOrgDocument fromExpandedResource(ExpandedResource resource) {
        return new SchemaOrgDocument(SchemaOrgType.SCHOLARLY_ARTICLE);
    }

    public SchemaOrgType getType() {
        return type;
    }
}
