package no.unit.nva;

import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.Contributor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Deprecated
class MigrateOtherToRoleOtherTest {

    public static Stream<String> roleOtherProvider() {
        return Stream.of(
            generateRoleOther(),
            generateLegacyOther(),
            generateRoleObjectWithRoleOther()
        );
    }

    @ParameterizedTest(name = "should accept legacy and current formatting for role other")
    @MethodSource("roleOtherProvider")
    void shouldMigrateOtherToRoleOther(String value) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(value, Contributor.class));
    }

    private static String generateRoleObjectWithRoleOther() {
        return "{\n"
                + "  \"type\": \"Contributor\",\n"
                + "  \"role\": {\n"
                + "    \"type\": \"RoleOther\"\n"
                + "  }\n"
                + "}";
    }

    private static String generateLegacyOther() {
        return "{\n"
                + "  \"type\" : \"Contributor\",\n"
                + "  \"role\" : \"Other\"\n"
                + "}";
    }

    private static String generateRoleOther() {
        return "{\n"
                + "  \"type\" : \"Contributor\",\n"
                + "  \"role\" : \"RoleOther\"\n"
                + "}";
    }
}
