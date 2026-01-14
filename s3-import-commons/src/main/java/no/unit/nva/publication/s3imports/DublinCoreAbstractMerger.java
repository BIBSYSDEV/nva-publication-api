package no.unit.nva.publication.s3imports;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DublinCoreAbstractMerger {

    private static final String ENGLISH = "en";
    private static final String NORWEGIAN = "nb";
    private static final String UNDEFINED = "und";
    private static final String ABSTRACT_SEPARATOR = "\n\n";

    private DublinCoreAbstractMerger() {
    }

    public static Map<String, String> mergeAbstracts(List<DcValue> dublinCoreAbstracts, String resourceAbstract,
                                                     Map<String, String> currentAlternativeAbstracts) {
        if (isNull(dublinCoreAbstracts) || dublinCoreAbstracts.isEmpty()) {
            return currentAlternativeAbstracts;
        }

        var updatedAlternativeAbstracts = new HashMap<>(currentAlternativeAbstracts);

        for (DcValue dcAbstract : dublinCoreAbstracts) {
            var abstractValue = dcAbstract.getValue().trim();
            if ((nonNull(resourceAbstract) && resourceAbstract.trim().equals(abstractValue))
                || currentAlternativeAbstracts.values().stream().anyMatch(value -> value.trim().equals(abstractValue))
                || updatedAlternativeAbstracts.values()
                       .stream()
                       .anyMatch(value -> value.trim().equals(abstractValue))) {
                continue;
            }

            var languageKey = mapLanguageToKey(dcAbstract.getLanguage());
            addOrAppendAbstract(updatedAlternativeAbstracts, languageKey, abstractValue);
        }

        return updatedAlternativeAbstracts;
    }

    private static void addOrAppendAbstract(Map<String, String> abstractsMap, String languageKey,
                                            String abstractValue) {
        abstractsMap.merge(languageKey, abstractValue,
                           (existing, newValue) -> existing + ABSTRACT_SEPARATOR + newValue);
    }

    private static String mapLanguageToKey(Language language) {
        return switch (language) {
            case ENGLISH -> ENGLISH;
            case NORWEGIAN -> NORWEGIAN;
            case null -> UNDEFINED;
        };
    }
}
