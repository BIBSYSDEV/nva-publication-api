package no.sikt.nva.scopus.conversion.model;

import static java.util.Objects.nonNull;
import static no.unit.nva.language.LanguageConstants.ENGLISH;
import no.unit.nva.language.LanguageMapper;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;

public final class LanguageUtil {

    private LanguageUtil() {
    }

    public static String guessTheLanguageOfTheInputStringAsIso6391Code(String textToBeGuessedLanguageCodeFrom) {
        var result = new OptimaizeLangDetector().loadModels().detect(textToBeGuessedLanguageCodeFrom);
        return result.isReasonablyCertain() ? getIso6391LanguageCodeForSupportedNvaLanguage(result.getLanguage())
                   : ENGLISH.getIso6391Code();
    }

    private static String getIso6391LanguageCodeForSupportedNvaLanguage(String possiblyUnsupportedLanguageIso6391code) {
        var language = LanguageMapper.getLanguageByIso6391Code(possiblyUnsupportedLanguageIso6391code);
        return nonNull(language.getIso6391Code()) ? language.getIso6391Code() : ENGLISH.getIso6391Code();
    }
}
