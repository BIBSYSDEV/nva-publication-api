package no.unit.nva.schemaorg;

import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum SchemaOrgType {
    SCHOLARLY_ARTICLE("ScholarlyArticle");

    private final String name;

    SchemaOrgType(String name) {

        this.name = name;
    }

    public String getName() {
        return name;
    }

    public SchemaOrgType lookup(String candidate) {
        return Arrays.stream(SchemaOrgType.values())
            .filter(item -> item.getName().equals(candidate))
            .collect(SingletonCollector.collect());
    }
}
