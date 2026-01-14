package no.unit.nva.publication.s3imports;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

public final class DublinCoreAbstractMerger {

    private static final String UNDEFINED = "und";
    private static final String ABSTRACT_SEPARATOR = "\n\n";

    private DublinCoreAbstractMerger() {
    }

    public static Map<String, String> mergeAbstracts(Collection<DcValue> dublinCoreAbstracts, String resourceAbstract,
                                                     Map<String, String> currentAlternativeAbstracts) {
        if (isNull(dublinCoreAbstracts) || dublinCoreAbstracts.isEmpty()) {
            return currentAlternativeAbstracts;
        }

        var updatedAlternativeAbstracts = new HashMap<>(currentAlternativeAbstracts);

        var existingAbstracts = new HashSet<String>();
        if (nonNull(resourceAbstract)) {
            existingAbstracts.add(resourceAbstract.trim());
        }
        currentAlternativeAbstracts.values().forEach(value -> existingAbstracts.add(value.trim()));

        for (DcValue dcAbstract : dublinCoreAbstracts) {
            var abstractValue = dcAbstract.getValue().trim();

            if (existingAbstracts.contains(abstractValue)) {
                continue;
            }

            var languageKey = Optional.ofNullable(dcAbstract.getLanguage()).map(Language::toNvaLanguage).orElse(UNDEFINED);
            addOrAppendAbstract(updatedAlternativeAbstracts, languageKey, abstractValue);
            existingAbstracts.add(abstractValue);
        }

        return updatedAlternativeAbstracts;
    }

    private static void addOrAppendAbstract(Map<String, String> abstractsMap, String languageKey,
                                            String abstractValue) {
        abstractsMap.merge(languageKey, abstractValue,
                           (existing, newValue) -> existing + ABSTRACT_SEPARATOR + newValue);
    }
}
