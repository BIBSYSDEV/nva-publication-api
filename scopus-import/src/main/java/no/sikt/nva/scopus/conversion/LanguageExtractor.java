package no.sikt.nva.scopus.conversion;

import static no.sikt.nva.scopus.ScopusConstants.UNKNOWN_LANGUAGE_DETECTED;
import static no.unit.nva.language.LanguageConstants.UNDEFINED_LANGUAGE;
import java.net.URI;
import java.util.List;
import no.scopus.generated.CitationLanguageTp;
import no.unit.nva.language.LanguageConstants;
import no.unit.nva.language.LanguageMapper;
import nva.commons.core.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LanguageExtractor {

    private final List<CitationLanguageTp> citationLanguageTps;
    private static final Logger logger = LoggerFactory.getLogger(LanguageExtractor.class);

    public LanguageExtractor(List<CitationLanguageTp> citationLanguageTps) {
        this.citationLanguageTps = citationLanguageTps;
    }

    public URI extractLanguage() {
        return switch (citationLanguageTps.size()) {
            case 0 -> UNDEFINED_LANGUAGE.getLexvoUri();
            case 1 -> convertToSupportedLanguage(citationLanguageTps.getFirst());
            default -> LanguageConstants.MULTIPLE.getLexvoUri();
        };
    }

    private URI convertToSupportedLanguage(CitationLanguageTp citationLanguageTp) {
        var languageCode = StringUtils.isBlank(citationLanguageTp.getLang())
                               ? StringUtils.EMPTY_STRING
                               : citationLanguageTp.getLang();
        var language = LanguageMapper.getLanguageByPotentialIsoCode(languageCode);
        if (UNDEFINED_LANGUAGE.equals(language)) {
            logger.info(String.format(
                UNKNOWN_LANGUAGE_DETECTED,
                citationLanguageTp.getLang(),
                citationLanguageTp.getLanguage()));
        }
        return language.getLexvoUri();
    }
}
