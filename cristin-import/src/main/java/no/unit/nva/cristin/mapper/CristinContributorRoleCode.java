package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedRoleException;

public enum CristinContributorRoleCode {
    CREATOR("FORFATTER"),
    EDITOR("REDAKT" + "\u00D8" + "R"), //REDAKTØR
    SUPERVISOR("VEILEDER"),
    PROGRAMME_PARTICIPANT("PROGRAMDELTAGER"),
    PROGRAMME_LEADER("PROGRAMLEDER"),
    RIGHTS_HOLDER("OPPHAVSMANN"),
    JOURNALIST("JOURNALIST"),
    EDITORIAL_BOARD_MEMBER("REDAKSJONSKOM"),
    INTERVIEW_SUBJECT("INTERVJUOBJEKT"),
    ACADEMIC_COORDINATOR("FAGLIG_ANSVARLIG");

    public static final String UNKNOWN_ROLE_ERROR = "Unmapped alias for roleCode: ";
    private final String value;

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
