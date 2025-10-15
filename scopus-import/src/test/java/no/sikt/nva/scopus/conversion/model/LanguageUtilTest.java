package no.sikt.nva.scopus.conversion.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class LanguageUtilTest {

    @Test
    void shouldGuessTheLanguage() {
        var value = "Sånn er Norge";
        assertEquals("nb", LanguageUtil.guessTheLanguageOfTheInputStringAsIso6391Code(value));
    }

    @Test
    void shouldSetLanguageToEnglishWhenUnableToGuess() {
        var value = "это er ± Norwegian";
        assertEquals("en", LanguageUtil.guessTheLanguageOfTheInputStringAsIso6391Code(value));
    }
}