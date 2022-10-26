package no.unit.nva.schemaorg;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import nva.commons.core.SingletonCollector;

import java.util.Arrays;

public enum SchemaOrgType {
    SCHOLARLY_ARTICLE("ScholarlyArticle");

    private final String name;

    SchemaOrgType(String name) {

        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

    @JsonCreator
    public SchemaOrgType lookup(String candidate) {
        return Arrays.stream(SchemaOrgType.values())
            .filter(item -> item.getName().equals(candidate))
            .collect(SingletonCollector.collect());
    }
}
