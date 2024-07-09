package no.unit.nva.model.instancetypes.journal;

import static no.unit.nva.model.instancetypes.journal.JournalArticleContentType.ACADEMIC_ARTICLE;
import static no.unit.nva.model.instancetypes.journal.JournalArticleContentType.ACADEMIC_LITERATURE_REVIEW;
import static no.unit.nva.model.instancetypes.journal.JournalArticleContentType.CASE_REPORT;
import static no.unit.nva.model.instancetypes.journal.JournalArticleContentType.POPULAR_SCIENCE_ARTICLE;
import static no.unit.nva.model.instancetypes.journal.JournalArticleContentType.PROFESSIONAL_ARTICLE;
import static no.unit.nva.model.instancetypes.journal.JournalArticleContentType.STUDY_PROTOCOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class JournalArticleContentTypeTest {

    @ParameterizedTest
    @EnumSource(JournalArticleContentType.class)
    void shouldReturnEnumerationWhenInputIsCurrentName(JournalArticleContentType journalArticleContentType)
        throws JsonProcessingException {
        var currentValue = "\"" + journalArticleContentType.getValue() + "\"";
        var output = JsonUtils.dtoObjectMapper.readValue(currentValue, JournalArticleContentType.class);
        assertEquals(output, journalArticleContentType);
    }

    @ParameterizedTest(name = "Should return JournalArticleContentType when value is {0}")
    @MethodSource("deprecatedJournalArticleContentTypeProvider")
    void shouldReturnEnumerationWhenInputIsDeprecatedName(String deprecatedValue,
                                                          JournalArticleContentType journalArticleContentType)
        throws JsonProcessingException {
        var deprecated = "\"" + deprecatedValue + "\"";
        var output = JsonUtils.dtoObjectMapper.readValue(deprecated, JournalArticleContentType.class);
        assertEquals(output, journalArticleContentType);
    }

    public static Stream<Arguments> deprecatedJournalArticleContentTypeProvider() {
        return Stream.of(
                Arguments.of("Research article", ACADEMIC_ARTICLE),
                Arguments.of("Review article", ACADEMIC_LITERATURE_REVIEW),
                Arguments.of("Case report", CASE_REPORT),
                Arguments.of("Study protocol", STUDY_PROTOCOL),
                Arguments.of("Professional article", PROFESSIONAL_ARTICLE),
                Arguments.of("Popular science article", POPULAR_SCIENCE_ARTICLE)
        );
    }
}