package no.unit.nva.model.instancetypes.chapter;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Content type for 'Chapter in Anthology' resource subtype when the creator chooses, 'Chapter' Resource type while
 * registering a publication.
 */
@Deprecated
public enum ChapterArticleContentType {
    ACADEMIC_CHAPTER("AcademicChapter", "Academic Chapter"),
    NON_FICTION_CHAPTER("NonFictionChapter", "Non-fiction Chapter"),
    POPULAR_SCIENCE_CHAPTER("PopularScienceChapter", "Popular Science Chapter"),
    TEXTBOOK_CHAPTER("TextbookChapter", "Textbook Chapter"),
    ENCYCLOPEDIA_CHAPTER("EncyclopediaChapter", "Encyclopedia Chapter"),
    /**
     * Introduction in anthology: Introductory chapter in an anthology.
     */
    INTRODUCTION("Introduction", "Introduction"),
    /**
     * Chapter in Exhibition catalogue: A chapter in an exhibition catalogue, if it is written as an anthology and not a
     * monograph.
     */
    EXHIBITION_CATALOG_CHAPTER("ExhibitionCatalogChapter", "Exhibition Catalog Chapter");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid ChapterContentType, expected one of: %s";
    public static final String DELIMITER = ", ";

    private final String value;
    private final String deprecatedValue;

    ChapterArticleContentType(String value, String deprecatedValue) {
        this.value = value;
        this.deprecatedValue = deprecatedValue;
    }

    @JsonCreator
    public static ChapterArticleContentType lookup(String value) {
        return stream(values())
            .filter(nameType -> equalsCurrentOrDeprecatedValue(value, nameType))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(createErrorMessage(value)));
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    private String getDeprecatedValue() {
        return deprecatedValue;
    }

    private static boolean equalsCurrentOrDeprecatedValue(String value, ChapterArticleContentType nameType) {
        return nameType.getValue().equalsIgnoreCase(value)
               || nameType.getDeprecatedValue().equalsIgnoreCase(value);
    }

    private static String createErrorMessage(String value) {
        return format(ERROR_MESSAGE_TEMPLATE, value, stream(ChapterArticleContentType.values())
            .map(ChapterArticleContentType::toString).collect(joining(DELIMITER)));
    }
}
