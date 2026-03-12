package no.unit.nva.model.validation;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@SuppressWarnings({"rawtypes", "unchecked"})
class ValidTextContentMapValuesValidatorTest {

    private final ValidTextContentMapValuesValidator validator = new ValidTextContentMapValuesValidator();

    @Test
    void shouldReturnTrueForNullMap() {
        assertThat(validator.isValid(null, null), is(true));
    }

    @Test
    void shouldReturnTrueForEmptyMap() {
        assertThat(validator.isValid(Collections.emptyMap(), null), is(true));
    }

    @Test
    void shouldReturnTrueForMapWithValidValues() {
        Map map = Map.of("en", "Hello World", "no", "Hei Verden");
        assertThat(validator.isValid(map, null), is(true));
    }

    @Test
    void shouldReturnTrueForMapWithNullValue() {
        Map map = new HashMap();
        map.put("en", null);
        assertThat(validator.isValid(map, null), is(true));
    }

    @Test
    void shouldReturnFalseForMapWithInvalidValue() {
        Map map = Map.of("en", "Valid text", "no", "Invalid \uFFFE text");
        assertThat(validator.isValid(map, null), is(false));
    }

    @Test
    void shouldReturnFalseWhenAnyValueContainsInvalidCharacter() {
        Map map = Map.of("en", "Hello World", "no", "\u0000");
        assertThat(validator.isValid(map, null), is(false));
    }
}
