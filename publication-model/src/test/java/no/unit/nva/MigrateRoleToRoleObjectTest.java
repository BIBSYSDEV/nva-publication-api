package no.unit.nva;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Contributor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Deprecated
public class MigrateRoleToRoleObjectTest {

    public static Stream<String> roleProvider() {
        return Stream.of(generateSimpleRoleContributorJson(), generateObjectRoleContributor());
    }

    @ParameterizedTest
    @MethodSource("roleProvider")
    void shouldMigrateAllRoleToRoleObject(String contributorJson) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(contributorJson, Contributor.class));
    }

    private static String generateObjectRoleContributor() {
        return "{\n"
               + "  \"type\": \"Contributor\",\n"
               + "  \"role\": {\n"
               + "    \"type\": \"AcademicCoordinator\"\n"
               + "  }\n"
               + "}";
    }

    private static String generateSimpleRoleContributorJson() {
        return "{\n"
               + "  \"type\" : \"Contributor\",\n"
               + "  \"role\" : \"AcademicCoordinator\"\n"
               + "}";
    }
}
