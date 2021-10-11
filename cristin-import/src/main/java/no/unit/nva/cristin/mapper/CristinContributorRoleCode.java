package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedRoleException;

import java.util.Arrays;

public enum CristinContributorRoleCode {
    CREATOR("FORFATTER"),
    EDITOR("REDAKT" + "\u00D8" + "R"), //REDAKTØR
    SUPERVISOR("VEILEDER");


    private final String value;

    public static final String UNKNOWN_ROLE_ERROR = "Unmapped alias for roleCode: ";

    CristinContributorRoleCode(String value) {
        this.value = value;
    }

    @JsonCreator
    public static CristinContributorRoleCode fromString(String roleCode) {
        return Arrays.stream(CristinContributorRoleCode.values())
            .filter(role -> role.getStringValue().equalsIgnoreCase(roleCode))
            .findAny()
            .orElseThrow(() -> new UnsupportedRoleException(UNKNOWN_ROLE_ERROR + roleCode));
    }

    @JsonValue
    public String getStringValue() {
        return value;
    }
}
