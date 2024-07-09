package no.unit.nva.model.time;

import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.model.time.duration.DefinedDuration;
import no.unit.nva.model.time.duration.Duration;
import no.unit.nva.model.time.duration.NullDuration;
import no.unit.nva.model.time.duration.UndefinedDuration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DurationTest {

    public static Stream<Arguments> durationsProvider() {
        return Stream.of(Arguments.of(NullDuration.create()),
                         Arguments.of(UndefinedDuration.fromValue(randomString())),
                         Arguments.of(DefinedDuration.builder()
                                          .withMinutes(randomInteger())
                                          .withHours(randomInteger())
                                          .withDays(randomInteger())
                                          .withWeeks(randomInteger())
                                          .build()));
    }

    @ParameterizedTest
    @MethodSource("durationsProvider")
    void shouldRoundTripDurations(Duration duration) throws JsonProcessingException {
        var json = JsonUtils.dtoObjectMapper.writeValueAsString(duration);
        var roundTrippedDuration = JsonUtils.dtoObjectMapper.readValue(json, Duration.class);

        assertEquals(duration, roundTrippedDuration);
    }
}
