package no.unit.nva.model.testing;

import static no.unit.nva.testutils.RandomDataGenerator.randomElement;
import no.unit.nva.language.Language;
import no.unit.nva.language.LanguageConstants;

import java.net.URI;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.language.LanguageDescription;

public final class RandomLanguageUtil {

    public static final Set<Language> unused = Set.of(LanguageConstants.NORWEGIAN, LanguageConstants.MISCELLANEOUS);

    private RandomLanguageUtil() {
    }

    public static String randomBcp47CompatibleLanguage() {
        var languages = getLanguageStream()
                            .map(LanguageDescription::getIso6391Code)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
        return randomElement(languages);
    }

    public static URI randomLexvoUri() {
        return getLanguageStream().map(LanguageDescription::getLexvoUri).filter(Objects::nonNull).findAny().orElseThrow();
    }

    private static Stream<LanguageDescription> getLanguageStream() {
        return LanguageConstants.ALL_LANGUAGES.stream().filter(RandomLanguageUtil::isUsedLanguageInNva);
    }

    private static boolean isUsedLanguageInNva(LanguageDescription f) {
        return !unused.contains(f);
    }
}
