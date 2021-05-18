package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;

public enum CristinSecondaryCategory {
    ANTHOLOGY;

    private static final Map<String, CristinSecondaryCategory> ALIASES_MAP = createAliasesMap();
    private static final Map<CristinSecondaryCategory, String> DEFAULT_NAMES_MAP = defaultNamesMap();

    @JsonCreator
    public static CristinSecondaryCategory fromString(String category) {
        return ALIASES_MAP.get(category);
    }

    @JsonValue
    public String getValue() {
        return DEFAULT_NAMES_MAP.get(this);
    }

    private static Map<String, CristinSecondaryCategory> createAliasesMap() {
        return Map.of("ANTOLOGI", ANTHOLOGY);
    }

    private static Map<CristinSecondaryCategory, String> defaultNamesMap() {
        return Map.of(ANTHOLOGY, "ANTOLOGI");
    }
}
