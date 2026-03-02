package no.unit.nva.model.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

class ValidTextContentValidatorTest {

    private final ValidTextContentValidator validator = new ValidTextContentValidator();

    @Test
    void shouldReturnTrueForNullValue() {
        assertThat(validator.isValid(null, null), is(true));
    }

    @Test
    void shouldReturnTrueForEmptyString() {
        assertThat(validator.isValid("", null), is(true));
    }

    @Test
    void shouldReturnTrueForRegularAsciiText() {
        assertThat(validator.isValid("Hello World", null), is(true));
    }

    @Test
    void shouldReturnTrueForUnicodeText() {
        assertThat(validator.isValid("Héllo Wörld 日本語 中文", null), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\t", "\n", "\r"})
    void shouldReturnTrueForAllowedControlCharacters(String input) {
        assertThat(validator.isValid(input, null), is(true));
    }

    @Test
    void shouldReturnTrueForCharacterAt0x20() {
        assertThat(validator.isValid(" ", null), is(true));
    }

    @Test
    void shouldReturnFalseForNullCharacter() {
        assertThat(validator.isValid("\u0000", null), is(false));
    }

    @Test
    void shouldReturnFalseForFFFECharacter() {
        assertThat(validator.isValid("\uFFFE", null), is(false));
    }

    @Test
    void shouldReturnFalseForFFFFCharacter() {
        assertThat(validator.isValid("\uFFFF", null), is(false));
    }

    @ParameterizedTest
    @ValueSource(ints = {0x01, 0x02, 0x08, 0x0B, 0x0C, 0x0E, 0x1F})
    void shouldReturnFalseForInvalidControlCharacters(int codePoint) {
        var input = new String(Character.toChars(codePoint));
        assertThat(validator.isValid(input, null), is(false));
    }

    @Test
    void shouldReturnTrueForTextWithValidCharactersInRange0xE000To0xFFFD() {
        assertThat(validator.isValid("\uE000", null), is(true));
        assertThat(validator.isValid("\uFFFD", null), is(true));
    }

    @Test
    void shouldReturnFalseForMixedTextContainingInvalidCharacter() {
        assertThat(validator.isValid("Valid text \uFFFE more text", null), is(false));
    }

    @Test
    void shouldReturnTrueForSupplementaryPlaneCharacters() {
        var emoji = "\uD83D\uDE00";
        assertThat(validator.isValid(emoji, null), is(true));
    }
}
