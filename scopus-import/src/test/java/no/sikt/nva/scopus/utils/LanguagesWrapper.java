package no.sikt.nva.scopus.utils;

import java.util.List;
import no.unit.nva.language.Language;

public class LanguagesWrapper {

    private final List<Language> languages;

    public LanguagesWrapper(List<Language> languages) {
        this.languages = languages;
    }

    public List<Language> getLanguages() {
        return languages;
    }
}
