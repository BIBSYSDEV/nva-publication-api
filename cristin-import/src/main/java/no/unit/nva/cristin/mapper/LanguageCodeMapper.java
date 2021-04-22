package no.unit.nva.cristin.mapper;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A very basic language mapper. This class will be expanded later.
 */
public final class LanguageCodeMapper {

    public static final URI ENGLISH_LANG_URI = URI.create("http://lexvo.org/id/iso639-3/eng");
    private static final Map<String, URI> languageCodes;

    static {
        languageCodes = new ConcurrentHashMap<>();
        languageCodes.put("nb", URI.create("http://lexvo.org/id/iso639-3/nob"));
        languageCodes.put("en", ENGLISH_LANG_URI);
        languageCodes.put("no", URI.create("http://lexvo.org/id/iso639-3/nor"));
    }

    private LanguageCodeMapper() {

    }

    public static URI parseLanguage(String code) {
        String lowerCased = code.toLowerCase(Locale.getDefault());
        return languageCodes.getOrDefault(lowerCased, ENGLISH_LANG_URI);
    }
}
