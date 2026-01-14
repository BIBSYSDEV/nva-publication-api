package no.unit.nva.publication.s3imports;

import static no.unit.nva.publication.s3imports.Language.ENGLISH;
import static no.unit.nva.publication.s3imports.Language.NORWEGIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DublinCoreAbstractMergerTest {

    @Test
    void shouldReturnEmptyMapWhenDublinCoreAbstractsIsNull() {
        var result = DublinCoreAbstractMerger.mergeAbstracts(null, null, Collections.emptyMap());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyMapWhenDublinCoreAbstractsIsEmpty() {
        var result = DublinCoreAbstractMerger.mergeAbstracts(List.of(), null, Collections.emptyMap());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotAddAbstractWhenItMatchesResourceAbstract() {
        var resourceAbstract = "This is the main abstract";
        var dcAbstracts = List.of(dcAbstract(resourceAbstract, ENGLISH));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, resourceAbstract, Collections.emptyMap());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotAddAbstractWhenItMatchesResourceAbstractWithDifferentWhitespace() {
        var resourceAbstract = "This is the main abstract";
        var dcAbstracts = List.of(dcAbstract("  This is the main abstract  ", ENGLISH));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, resourceAbstract, Collections.emptyMap());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotAddAbstractWhenItExistsInAlternativeAbstracts() {
        var existingAlternative = "Existing alternative abstract";
        var currentAlternatives = Map.of("en", existingAlternative);
        var dcAbstracts = List.of(dcAbstract(existingAlternative, ENGLISH));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, currentAlternatives);

        assertEquals(1, result.size());
        assertEquals(existingAlternative, result.get("en"));
    }

    @Test
    void shouldAddNewAbstractWithEnglishLanguage() {
        var newAbstract = "New English abstract";
        var dcAbstracts = List.of(dcAbstract(newAbstract, ENGLISH));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, Collections.emptyMap());

        assertEquals(1, result.size());
        assertEquals(newAbstract, result.get("en"));
    }

    @Test
    void shouldAddNewAbstractWithNorwegianLanguage() {
        var newAbstract = "Nytt norsk sammendrag";
        var dcAbstracts = List.of(dcAbstract(newAbstract, NORWEGIAN));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, Collections.emptyMap());

        assertEquals(1, result.size());
        assertEquals(newAbstract, result.get("nb"));
    }

    @Test
    void shouldAddNewAbstractWithUndefinedLanguageWhenLanguageIsNull() {
        var newAbstract = "Abstract without language";
        var dcAbstracts = List.of(dcAbstract(newAbstract, null));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, Collections.emptyMap());

        assertEquals(1, result.size());
        assertEquals(newAbstract, result.get("und"));
    }

    @Test
    void shouldAddMultipleAbstractsWithDifferentLanguages() {
        var englishAbstract = "English abstract";
        var norwegianAbstract = "Norsk sammendrag";
        var dcAbstracts = List.of(
            dcAbstract(englishAbstract, ENGLISH),
            dcAbstract(norwegianAbstract, NORWEGIAN)
        );

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, Collections.emptyMap());

        assertEquals(2, result.size());
        assertEquals(englishAbstract, result.get("en"));
        assertEquals(norwegianAbstract, result.get("nb"));
    }

    @Test
    void shouldMergeMultipleAbstractsWithSameLanguage() {
        var firstAbstract = "First abstract";
        var secondAbstract = "Second abstract";
        var dcAbstracts = List.of(
            dcAbstract(firstAbstract, ENGLISH),
            dcAbstract(secondAbstract, ENGLISH)
        );

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, Collections.emptyMap());

        assertEquals(1, result.size());
        assertEquals(firstAbstract + "\n\n" + secondAbstract, result.get("en"));
    }

    @Test
    void shouldPreserveExistingAlternativeAbstractsWhenAddingNew() {
        var existingAbstract = "Existing abstract";
        var newAbstract = "New abstract";
        var currentAlternatives = Map.of("nb", existingAbstract);
        var dcAbstracts = List.of(dcAbstract(newAbstract, ENGLISH));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, currentAlternatives);

        assertEquals(2, result.size());
        assertEquals(existingAbstract, result.get("nb"));
        assertEquals(newAbstract, result.get("en"));
    }

    @Test
    void shouldAppendToExistingAlternativeAbstractWhenSameLanguage() {
        var existingAbstract = "Existing abstract";
        var newAbstract = "New abstract";
        var currentAlternatives = Map.of("en", existingAbstract);
        var dcAbstracts = List.of(dcAbstract(newAbstract, ENGLISH));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, currentAlternatives);

        assertEquals(1, result.size());
        assertEquals(existingAbstract + "\n\n" + newAbstract, result.get("en"));
    }

    @Test
    void shouldSkipDuplicateAbstractsWhenMultipleWithSameContent() {
        var duplicateAbstract = "Same abstract";
        var dcAbstracts = List.of(dcAbstract(duplicateAbstract, ENGLISH), dcAbstract(duplicateAbstract, ENGLISH));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, Collections.emptyMap());

        assertEquals(1, result.size());
        assertEquals(duplicateAbstract, result.get("en"));
    }

    @Test
    void shouldNotAddAbstractThatMatchesExistingAlternativeAbstractWithDifferentLanguageKey() {
        var abstractText = "Same abstract text";
        var currentAlternatives = Map.of("nb", abstractText);
        var dcAbstracts = List.of(dcAbstract(abstractText, ENGLISH));

        var result = DublinCoreAbstractMerger.mergeAbstracts(dcAbstracts, null, currentAlternatives);

        assertEquals(1, result.size());
        assertEquals(abstractText, result.get("nb"));
    }

    private static DcValue dcAbstract(String value, Language language) {
        var dcValue = new DcValue(Element.DESCRIPTION, Qualifier.ABSTRACT, value, language);
        dcValue.setLanguage(language);
        return dcValue;
    }
}
