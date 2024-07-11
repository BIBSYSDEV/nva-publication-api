package no.unit.nva.model.instancetypes.chapter;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.ACADEMIC_CHAPTER;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.DELIMITER;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.ENCYCLOPEDIA_CHAPTER;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.ERROR_MESSAGE_TEMPLATE;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.EXHIBITION_CATALOG_CHAPTER;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.INTRODUCTION;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.NON_FICTION_CHAPTER;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.POPULAR_SCIENCE_CHAPTER;
import static no.unit.nva.model.instancetypes.chapter.ChapterArticleContentType.TEXTBOOK_CHAPTER;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.util.stream.Stream;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ChapterArticleContentTypeTest {

    public static Stream<Arguments> deprecatedValuesProvider() {
        return Stream.of(
            Arguments.of(ACADEMIC_CHAPTER, "Academic Chapter"),
            Arguments.of(NON_FICTION_CHAPTER, "Non-fiction Chapter"),
            Arguments.of(POPULAR_SCIENCE_CHAPTER, "Popular Science Chapter"),
            Arguments.of(TEXTBOOK_CHAPTER, "Textbook Chapter"),
            Arguments.of(ENCYCLOPEDIA_CHAPTER, "Encyclopedia Chapter"),
            Arguments.of(INTRODUCTION, "Introduction"),
            Arguments.of(EXHIBITION_CATALOG_CHAPTER, "Exhibition Catalog Chapter")
        );
    }

    @ParameterizedTest
    @EnumSource(ChapterArticleContentType.class)
    void shouldReturnChapterArticleContentTypeWhenInputIsCurrentValue(
        ChapterArticleContentType chapterArticleContentType)
        throws JsonProcessingException {
        var currentValue = "\"" + chapterArticleContentType.getValue() + "\"";
        var expectedChapterArticleContentType = JsonUtils.dtoObjectMapper.readValue(currentValue,
                                                                                    ChapterArticleContentType.class);
        assertEquals(expectedChapterArticleContentType, chapterArticleContentType);
    }

    @ParameterizedTest(name = "should return ChapterArticleContentType when input is {0}")
    @MethodSource("deprecatedValuesProvider")
    void shouldReturnChapterArticleContentTypeWhenInputIsDeprecatedValue(
        ChapterArticleContentType chapterArticleContentType,String deprecatedValue)
        throws JsonProcessingException {
        var deprecated = "\"" + deprecatedValue + "\"";
        var expectedChapterArticleContentType = JsonUtils.dtoObjectMapper.readValue(deprecated,
                                                                                    ChapterArticleContentType.class);
        assertEquals(expectedChapterArticleContentType, chapterArticleContentType);
    }

    @Test
    void shouldThrowErrorWithProperErrorMessageWhenInvalidInputValueSupplied() {
        var invalidInput = randomString();
        var inputJsonValue = "\"" + invalidInput + "\"";
        Executable handleRequest = () -> JsonUtils.dtoObjectMapper.readValue(inputJsonValue,
                                                                             ChapterArticleContentType.class);

        var response = assertThrows(ValueInstantiationException.class, handleRequest);
        var actualErrorMessage = createErrorMessage(invalidInput);
        assertThat(response.getMessage(), containsString(actualErrorMessage));
    }

    private static String createErrorMessage(String value) {
        return format(ERROR_MESSAGE_TEMPLATE, value, stream(ChapterArticleContentType.values())
            .map(ChapterArticleContentType::toString).collect(joining(DELIMITER)));
    }
}