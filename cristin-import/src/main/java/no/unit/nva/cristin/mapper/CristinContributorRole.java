package no.unit.nva.cristin.mapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import no.unit.nva.model.Role;

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
    
    @JsonProperty("rollekode")
    private CristinContributorRoleCode roleCode;
    
    public CristinContributorRole() {
    }
    
    public Role toNvaRole() {
        if (CristinContributorRoleCode.CREATOR.equals(roleCode)) {
            return Role.CREATOR;
        } else if (CristinContributorRoleCode.EDITOR.equals(roleCode)) {
            return Role.EDITOR;
        } else if (CristinContributorRoleCode.SUPERVISOR.equals(roleCode)) {
            return Role.SUPERVISOR;
        } else if (CristinContributorRoleCode.PROGRAMME_PARTICIPANT.equals(roleCode)) {
            return Role.PROGRAMME_PARTICIPANT;
        } else if (CristinContributorRoleCode.PROGRAMME_LEADER.equals(roleCode)) {
            return Role.PROGRAMME_LEADER;
        } else if (CristinContributorRoleCode.RIGHTS_HOLDER.equals(roleCode)) {
            return Role.RIGHTS_HOLDER;
        } else if (CristinContributorRoleCode.JOURNALIST.equals(roleCode)) {
            return Role.JOURNALIST;
        } else if (CristinContributorRoleCode.EDITORIAL_BOARD_MEMBER.equals(roleCode)) {
            return Role.EDITORIAL_BOARD_MEMBER;
        } else if (CristinContributorRoleCode.INTERVIEW_SUBJECT.equals(roleCode)) {
            return Role.INTERVIEW_SUBJECT;
        } else if (CristinContributorRoleCode.ACADEMIC_COORDINATOR.equals(roleCode)) {
            return Role.ACADEMIC_COORDINATOR;
        } else {
            return Role.OTHER;
        }
    }
}
