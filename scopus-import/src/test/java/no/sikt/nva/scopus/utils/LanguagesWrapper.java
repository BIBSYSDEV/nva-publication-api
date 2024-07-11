package no.sikt.nva.scopus.utils;

import java.util.List;
import no.unit.nva.language.LanguageDescription;

public class LanguagesWrapper {

    private final List<LanguageDescription> languages;

    public LanguagesWrapper(List<LanguageDescription> languages) {
        this.languages = languages;
    }

    public List<LanguageDescription> getLanguages() {
        return languages;
    }
}
