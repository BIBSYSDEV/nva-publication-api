package no.unit.nva.publication.model.business;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ThirdPartySystemTest {

    @ParameterizedTest
    @MethodSource("thirdPartySystemProvider")
    void shouldConvertKnownThirdPartySystemToEnumWithoutBeingCaseSensitive(String value, ThirdPartySystem expected) {
        assertEquals(expected, ThirdPartySystem.fromValue(value));
    }

    @Test
    void shouldConvertUnknownValueToOther() {
        assertEquals(ThirdPartySystem.OTHER, ThirdPartySystem.fromValue(randomString()));
    }

    private static Stream<Arguments> thirdPartySystemProvider() {
        return Stream.of(Arguments.of(" wiSEfloW ", ThirdPartySystem.WISE_FLOW),
                         Arguments.of(" INSperA ", ThirdPartySystem.INSPERA));
    }
}