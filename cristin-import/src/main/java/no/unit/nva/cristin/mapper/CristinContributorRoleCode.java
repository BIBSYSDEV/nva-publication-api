package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public enum CristinContributorRoleCode {
    CREATOR,
    EDITOR;

    public static final String EDITOR_NOR = "REDAKTØR";
    public static final String CREATOR_NOR = "FORFATTER";
    public static final String UNKNOWN_ROLE_ERROR = "Unmapped alias for roleCode: ";
    private static final Map<String, CristinContributorRoleCode> ALIASES_MAP = constructAliasesMap();
    private static final Map<CristinContributorRoleCode, String> DEFAULT_NAMES = constructDefaultNames();

    @JsonCreator
    public static CristinContributorRoleCode fromString(String roleCode) {
        CristinContributorRoleCode returnValue = Optional.ofNullable(ALIASES_MAP.get(roleCode))
            .orElseThrow(() -> new RuntimeException(UNKNOWN_ROLE_ERROR + roleCode));
        return returnValue;
    }

    @JsonValue
    public String getStringValue() {
        return DEFAULT_NAMES.get(this);
    }

    private static Map<CristinContributorRoleCode, String> constructDefaultNames() {
        return Map.of(
            EDITOR, EDITOR_NOR,
            CREATOR, CREATOR_NOR);
    }

    private static Map<String, CristinContributorRoleCode> constructAliasesMap() {
        Map<String, CristinContributorRoleCode> aliasesMap = new ConcurrentHashMap<>();
        aliasesMap.put(EDITOR_NOR, EDITOR);
        aliasesMap.put(CREATOR_NOR, CREATOR);
        return Collections.unmodifiableMap(aliasesMap);
    }
}
