package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;

public enum CristinMainCategory {
    BOOK;

    private static final Map<String, CristinMainCategory> ALIASES_MAP = createAliasesMap();
    private static final Map<CristinMainCategory, String> DEFAULT_NAMES_MAP = defaultNamesMap();

    @JsonCreator
    public static CristinMainCategory fromString(String category) {
        return ALIASES_MAP.get(category);
    }

    @JsonValue
    public String getValue() {
        return DEFAULT_NAMES_MAP.get(this);
    }

    private static Map<String, CristinMainCategory> createAliasesMap() {
        return Map.of("BOK", BOOK);
    }

    private static Map<CristinMainCategory, String> defaultNamesMap() {
        return Map.of(BOOK, "BOK");
    }
}
