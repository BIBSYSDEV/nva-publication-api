package no.unit.nva.model.time;

import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@SuppressWarnings({"checkstyle:*"})
@Deprecated
class TimeMigrationTest {

    @ParameterizedTest(name = "Should accept value {0}")
    @ValueSource(strings = {"{\"type\": \"Instant\", \"value\": \"2022-03-23T00:00:00\"}{\n"
                    + "  \"type\": \"Period\",\n"
                    + "  \"from\": \"2022-03-23T00:00:00\",\n"
                    + "  \"to\": \"2022-03-23T00:00:00\"\n"
                    + "}", "{\"type\": \"Instant\", \"value\": \"2022-03-23T00:00:00.000000Z\"}"
                    + "{\n\"type\": \"Period\",\n"
                    + "  \"from\": \"2022-03-23T00:00:00.000000Z\",\n"
                    + "  \"to\": \"2022-03-23T00:00:00.000000Z\"\n"
                    + "}"
    })
    void shouldConvertLocalDateToInstant(String value) {
        assertDoesNotThrow(() -> JsonUtils.dtoObjectMapper.readValue(value, Time.class));
    }
}