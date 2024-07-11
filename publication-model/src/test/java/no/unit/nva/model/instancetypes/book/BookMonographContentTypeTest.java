package no.unit.nva.model.instancetypes.book;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.model.instancetypes.book.BookMonographContentType.ACADEMIC_MONOGRAPH;
import static no.unit.nva.model.instancetypes.book.BookMonographContentType.DELIMITER;
import static no.unit.nva.model.instancetypes.book.BookMonographContentType.ERROR_MESSAGE_TEMPLATE;
import static no.unit.nva.model.instancetypes.book.BookMonographContentType.NON_FICTION_MONOGRAPH;
import static no.unit.nva.model.instancetypes.book.BookMonographContentType.POPULAR_SCIENCE_MONOGRAPH;
import static no.unit.nva.model.instancetypes.book.BookMonographContentType.values;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class BookMonographContentTypeTest {

    public static Stream<Arguments> deprecatedValuesProvider() {
        return Stream.of(Arguments.of("Academic Monograph", ACADEMIC_MONOGRAPH),
                         Arguments.of("Non-fiction Monograph", NON_FICTION_MONOGRAPH),
                         Arguments.of("Popular Science Monograph", POPULAR_SCIENCE_MONOGRAPH)
        );
    }

    @ParameterizedTest
    @EnumSource(BookMonographContentType.class)
    void shouldReturnBookMonographContentTypeWhenInputIsCurrentValue(BookMonographContentType bookMonographContentType)
        throws JsonProcessingException {
        var currentValue = "\"" + bookMonographContentType.getValue() + "\"";
        var expectedBookMonographContentType = JsonUtils.dtoObjectMapper.readValue(currentValue,
                                                                                   BookMonographContentType.class);
        assertEquals(expectedBookMonographContentType, bookMonographContentType);
    }

    @ParameterizedTest(name = "should return BookMonographContentType when input is {0}")
    @MethodSource("deprecatedValuesProvider")
    void shouldReturnBookMonographContentTypeWhenInputIsDeprecatedValue(
        String deprecatedValue,
        BookMonographContentType bookMonographContentType)
        throws JsonProcessingException {
        var deprecated = "\"" + deprecatedValue + "\"";
        var expectedBookMonographContentType = JsonUtils.dtoObjectMapper.readValue(deprecated,
                                                                                   BookMonographContentType.class);
        assertEquals(expectedBookMonographContentType, bookMonographContentType);
    }

    @Test
    void shouldThrowErrorWithProperErrorMessageWhenInvalidInputValueSupplied() {
        var invalidInput = randomString();
        var inputJsonValue = "\"" + invalidInput + "\"";
        Executable handleRequest = () -> JsonUtils.dtoObjectMapper.readValue(inputJsonValue,
                                                                             BookMonographContentType.class);

        var response = assertThrows(ValueInstantiationException.class, handleRequest);
        var actualErrorMessage = createErrorMessage(invalidInput);
        assertThat(response.getMessage(), containsString(actualErrorMessage));
    }

    private static String createErrorMessage(String value) {
        return format(ERROR_MESSAGE_TEMPLATE, value, stream(values())
            .map(BookMonographContentType::toString).collect(joining(DELIMITER)));
    }
}