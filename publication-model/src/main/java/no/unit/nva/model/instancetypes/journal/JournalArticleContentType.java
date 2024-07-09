package no.unit.nva.model.instancetypes.journal;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@Deprecated
public enum JournalArticleContentType {
    ACADEMIC_ARTICLE("AcademicArticle", "Research article"),
    ACADEMIC_LITERATURE_REVIEW("AcademicLiteratureReview", "Review article"),
    CASE_REPORT("CaseReport", "Case report"),
    STUDY_PROTOCOL("StudyProtocol", "Study protocol"),
    PROFESSIONAL_ARTICLE("ProfessionalArticle", "Professional article"),
    POPULAR_SCIENCE_ARTICLE("PopularScienceArticle", "Popular science article");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid JournalArticleContentType, expected one of: %s";
    public static final String DELIMITER = ", ";

    private final String value;
    private final String deprecatedValue;

    JournalArticleContentType(String value, String deprecatedValue) {
        this.deprecatedValue = deprecatedValue;
        this.value = value;
    }

    @JsonCreator
    public static JournalArticleContentType lookup(String value) {
        return stream(values())
            .filter(nameType -> equalsCurrentOrDeprecatedValue(value, nameType))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException(createErrorMessage(value)));
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Deprecated
    private String getDeprecatedValue() {
        return deprecatedValue;
    }

    private static boolean equalsCurrentOrDeprecatedValue(String value, JournalArticleContentType nameType) {
        return nameType.getValue().equalsIgnoreCase(value)
               || nameType.getDeprecatedValue().equalsIgnoreCase(value);
    }

    private static String createErrorMessage(String value) {
        return format(ERROR_MESSAGE_TEMPLATE, value, stream(JournalArticleContentType.values())
            .map(JournalArticleContentType::toString).collect(joining(DELIMITER)));
    }
}
