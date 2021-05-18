package no.unit.nva.cristin.mapper;

import static no.unit.nva.publication.PublicationGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}