package no.unit.nva.model.validation;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@SuppressWarnings({"rawtypes", "unchecked"})
class ValidTextContentListValuesValidatorTest {

    private final ValidTextContentListValuesValidator validator = new ValidTextContentListValuesValidator();

    @Test
    void shouldReturnTrueForNullList() {
        assertThat(validator.isValid(null, null), is(true));
    }

    @Test
    void shouldReturnTrueForEmptyList() {
        assertThat(validator.isValid(Collections.emptyList(), null), is(true));
    }

    @Test
    void shouldReturnTrueForListWithValidValues() {
        Collection list = List.of("Hello", "World", "Test");
        assertThat(validator.isValid(list, null), is(true));
    }

    @Test
    void shouldReturnTrueForListWithNullElement() {
        Collection list = Arrays.asList("Hello", null, "World");
        assertThat(validator.isValid(list, null), is(true));
    }

    @Test
    void shouldReturnFalseForListWithInvalidValue() {
        Collection list = List.of("Valid text", "Invalid \uFFFE text");
        assertThat(validator.isValid(list, null), is(false));
    }

    @Test
    void shouldReturnFalseWhenAnyElementContainsInvalidCharacter() {
        Collection list = List.of("Hello", "World", "\u0000");
        assertThat(validator.isValid(list, null), is(false));
    }
}
