package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;

public enum CristinSecondaryCategory {
    ANTHOLOGY, TEMPORARILY_UNKNOWN, UNMAPPED;

    private static final Map<String, CristinSecondaryCategory> KNOWN_ALIASES_MAP = knownAliases();
    private static final Map<CristinSecondaryCategory, String> DEFAULT_NAMES_MAP = defaultNamesMap();

    @JsonCreator
    public static CristinSecondaryCategory fromString(String category) {
        return KNOWN_ALIASES_MAP.getOrDefault(category, UNMAPPED);
    }

    @JsonValue
    public String getValue() {
        return DEFAULT_NAMES_MAP.get(this);
    }

    public boolean isUnknownCategory() {
        return UNMAPPED.equals(this);
    }

    private static Map<String, CristinSecondaryCategory> knownAliases() {
        return Map.of("ANTOLOGI", ANTHOLOGY);
    }

    private static Map<CristinSecondaryCategory, String> defaultNamesMap() {
        return Map.of(ANTHOLOGY, "ANTOLOGI",
                      TEMPORARILY_UNKNOWN, "TEMPORARILY_UNKNOWN",
                      UNMAPPED, "UNMAPPED");
    }
}
