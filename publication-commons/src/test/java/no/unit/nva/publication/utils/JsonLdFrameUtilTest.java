package no.unit.nva.publication.utils;

import static nva.commons.core.attempt.Try.attempt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Named.named;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JsonLdFrameUtilTest {

    public static final String VALID_CONTEXT = """
        {
          "@vocab": "https://example.com/vocab",
          "id": "@id",
          "type": "@type"
        }
        """;
    public static final String VALID_FRAME_NO_CONTEXT = """
        {
          "@type": "Publication"
        }
        """;

    public static final String VALID_FRAME_WITH_CONTEXT = """
        {
          "@context": "https://example.com/context",
          "@type": "Publication"
        }
        """;

    public static Stream<Named<Pair>> badJsonProvider() {
        return Stream.of(
            named("Bad frame", new Pair("null", VALID_CONTEXT)),
            named("Bad context", new Pair(VALID_FRAME_NO_CONTEXT, "null")),
            named("Invalid frame", new Pair("{()->", VALID_CONTEXT)),
            named("Invalid context", new Pair(VALID_FRAME_WITH_CONTEXT, "{()->"))
        );
    }

    public static Stream<Named<String>> frameProvider() {
        return Stream.of(
            named("Frame without context", VALID_FRAME_NO_CONTEXT),
            named("Frame with context", VALID_FRAME_WITH_CONTEXT)
        );
    }

    @ParameterizedTest
    @DisplayName("Should allow frames with and without contexts")
    @MethodSource("frameProvider")
    void shouldAllowFramesWithAndWithoutContexts(String frame) {
        var expectedString = """
            {
              "@context": {
                "@vocab": "https://example.com/vocab",
                "id": "@id",
                "type": "@type"
              },
              "@type": "Publication"
            }
            """;

        var expected = attempt(() -> JsonUtils.dtoObjectMapper.readTree(expectedString)).orElseThrow();
        var actual = JsonLdFrameUtil.from(frame, VALID_CONTEXT);
        assertThat(actual, is(equalTo(expected)));
    }


    @ParameterizedTest
    @DisplayName("Should throw when JSON input is invalid")
    @MethodSource("badJsonProvider")
    void shouldThrowWhenInputJsonStringIsInvalid(Pair pair) {
        ThrowingRunnable throwingRunnable = () -> JsonLdFrameUtil.from(pair.frame(), pair.context());
        assertThrows(IllegalArgumentException.class, throwingRunnable);
    }

    record Pair(String frame, String context) {
    }
}