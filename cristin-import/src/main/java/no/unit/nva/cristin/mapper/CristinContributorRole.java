package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;

@Data
@Builder(
    builderClassName = "CristinContributorRoleBuilder",
    toBuilder = true,
    builderMethodName = "builder",
    buildMethodName = "build",
    setterPrefix = "with"
)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class CristinContributorRole {

    @JsonIgnore
    private static Map<CristinContributorRoleCode, Role> cristinRoleToNvaRoles = Map.ofEntries(
        new SimpleEntry<>(CristinContributorRoleCode.CREATOR, Role.CREATOR),
        new SimpleEntry<>(CristinContributorRoleCode.EDITOR, Role.EDITOR),
        new SimpleEntry<>(CristinContributorRoleCode.SUPERVISOR, Role.SUPERVISOR),
        new SimpleEntry<>(CristinContributorRoleCode.PROGRAMME_PARTICIPANT, Role.PROGRAMME_PARTICIPANT),
        new SimpleEntry<>(CristinContributorRoleCode.PROGRAMME_LEADER, Role.PROGRAMME_LEADER),
        new SimpleEntry<>(CristinContributorRoleCode.RIGHTS_HOLDER, Role.RIGHTS_HOLDER),
        new SimpleEntry<>(CristinContributorRoleCode.JOURNALIST, Role.JOURNALIST),
        new SimpleEntry<>(CristinContributorRoleCode.EDITORIAL_BOARD_MEMBER, Role.EDITORIAL_BOARD_MEMBER),
        new SimpleEntry<>(CristinContributorRoleCode.INTERVIEW_SUBJECT, Role.INTERVIEW_SUBJECT),
        new SimpleEntry<>(CristinContributorRoleCode.ACADEMIC_COORDINATOR, Role.ACADEMIC_COORDINATOR),
        new SimpleEntry<>(CristinContributorRoleCode.ARTIST, Role.ARTIST),
        new SimpleEntry<>(CristinContributorRoleCode.ARCHITECT, Role.ARCHITECT),
        new SimpleEntry<>(CristinContributorRoleCode.COMPOSER, Role.COMPOSER),
        new SimpleEntry<>(CristinContributorRoleCode.CONDUCTOR, Role.CONDUCTOR),
        new SimpleEntry<>(CristinContributorRoleCode.CONTRIBUTOR, Role.OTHER),
        new SimpleEntry<>(CristinContributorRoleCode.CURATOR, Role.CURATOR),
        new SimpleEntry<>(CristinContributorRoleCode.ORGANIZER, Role.ORGANIZER),
        new SimpleEntry<>(CristinContributorRoleCode.PERFORMER, Role.ARTIST),
        new SimpleEntry<>(CristinContributorRoleCode.TRANSLATOR, Role.TRANSLATOR_ADAPTER));

    @JsonProperty("rollekode")
    private CristinContributorRoleCode roleCode;

    public CristinContributorRole() {
    }

    public RoleType toNvaRole() {
        return new RoleType(cristinRoleToNvaRoles.getOrDefault(roleCode, Role.OTHER));
    }
}
