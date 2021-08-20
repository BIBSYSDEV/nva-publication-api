package no.unit.nva.cristin.mapper;

import nva.commons.core.SingletonCollector;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public enum ContentType {

    ARTIKKEL_FAG("ARTIKKEL_FAG", "Professional article"),
    ARTIKKEL_POP("ARTIKKEL_POP", "Popular science article"),
    ARTIKKEL("ARTIKKEL", "Research article"),
    OVERSIKTSART("OVERSIKTSART", "Review article"),
    UNMAPPED;

    private final List<String> aliases;
    public static final int CONTENT_TYPE = 1;

    ContentType(String... aliases) {
        this.aliases = Arrays.asList(aliases);
    }


    public static ContentType fromString(String category) {
        return Arrays.stream(values())
                .filter(enumValue -> enumValue.aliases.contains(category))
                .collect(SingletonCollector.collectOrElse(UNMAPPED));
    }

    public String retrieveContentTypeValue() {
        if (Objects.nonNull(aliases) && !aliases.isEmpty()) {
            return aliases.get(CONTENT_TYPE);
        }
        return this.name();
    }
}
