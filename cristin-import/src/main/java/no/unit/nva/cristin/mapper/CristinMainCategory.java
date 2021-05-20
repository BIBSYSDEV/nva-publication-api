package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;

public enum CristinMainCategory {
    BOOK, UNMAPPED;

    private static final Map<String, CristinMainCategory> KNOWN_ALIASES_MAP = createKnownAliasesMap();
    private static final Map<CristinMainCategory, String> DEFAULT_NAMES_MAP = defaultNamesMap();

    @JsonCreator
    public static CristinMainCategory fromString(String category) {
        return KNOWN_ALIASES_MAP.getOrDefault(category, UNMAPPED);
    }

    @JsonValue
    public String getValue() {
        return DEFAULT_NAMES_MAP.get(this);
    }

    public boolean isUnknownCategory() {
        return UNMAPPED.equals(this);
    }

    private static Map<String, CristinMainCategory> createKnownAliasesMap() {
        return Map.of("BOK", BOOK);
    }

    private static Map<CristinMainCategory, String> defaultNamesMap() {
        return Map.of(BOOK, "BOK",
                      UNMAPPED, "UNMAPPED");
    }
}
