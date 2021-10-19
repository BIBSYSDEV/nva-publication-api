package no.unit.nva.cristin.mapper;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.nio.file.Path;
import java.util.List;
import no.unit.nva.testutils.IoUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class CristinContributorRoleCodeTest {

    @Test
    public void fromStringShouldThrowExceptionWithInvalidRoleWhenInvalidRoleIsProvided() {
        String roleCode = randomString();
        Executable action = () -> CristinContributorRoleCode.fromString(roleCode);
        RuntimeException exception = assertThrows(RuntimeException.class, action);
        assertThat(exception.getMessage(), containsString(roleCode));
    }

    @Test
    public void fromStringShouldReturnEditorWhenInputIsRedaktor() {
        String roleCode = "REDAKTØR";
        CristinContributorRoleCode role = CristinContributorRoleCode.fromString(roleCode);
        assertThat(role, is(equalTo(CristinContributorRoleCode.EDITOR)));
    }

    @Test
    public void fromStringShouldReturnAcademicCoordinatorWhenTheInputIsFagligAnsvarlig() {
        String roldeCode = "FAGLIG_ANSVARLIG";
        CristinContributorRoleCode role = CristinContributorRoleCode.fromString(roldeCode);
        assertThat(role, is(equalTo(CristinContributorRoleCode.ACADEMIC_COORDINATOR)));
    }

    @Test
    public void fromStringReturnsEnumValueWhenInputIsValidAndIsUtf8() {
        List<String> roleCodes = IoUtils.linesfromResource(Path.of("contributer_role_codes.txt"));
        for (String roleCode : roleCodes) {
            CristinContributorRoleCode role = CristinContributorRoleCode.fromString(roleCode);
            assertThat(role.getStringValue(), is(equalTo(roleCode)));
        }
    }
}