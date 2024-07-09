package no.unit.nva.model.instancetypes.book;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Content Type options for "Monograph" subtype when the creator chooses the Resource type as "Book" while doing the
 * registration.
 */
@Deprecated
public enum BookMonographContentType {
    ACADEMIC_MONOGRAPH("AcademicMonograph", "Academic Monograph"),
    NON_FICTION_MONOGRAPH("NonFictionMonograph", "Non-fiction Monograph"),
    POPULAR_SCIENCE_MONOGRAPH("PopularScienceMonograph", "Popular Science Monograph"),
    TEXTBOOK("Textbook", "Textbook"),
    ENCYCLOPEDIA("Encyclopedia", "Encyclopedia"),
    /**
     * Enum Type Exhibition catalogue represents: A book published for a specific art or museum exhibition. Contains a
     * list of exhibits at the exhibition.
     */
    EXHIBITION_CATALOG("ExhibitionCatalog", "Exhibition catalog");

    public static final String ERROR_MESSAGE_TEMPLATE = "%s not a valid BookMonographContentType, expected one of: %s";
    public static final String DELIMITER = ", ";

    private final String value;
    private final String deprecatedValue;

    BookMonographContentType(String value, String deprecatedValue) {
        this.value = value;
        this.deprecatedValue = deprecatedValue;
    }

    @JsonCreator
    public static BookMonographContentType lookup(String value) {
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

    private static boolean equalsCurrentOrDeprecatedValue(String value, BookMonographContentType nameType) {
        return nameType.getValue().equalsIgnoreCase(value)
               || nameType.getDeprecatedValue().equalsIgnoreCase(value);
    }

    private static String createErrorMessage(String value) {
        return format(ERROR_MESSAGE_TEMPLATE, value, stream(BookMonographContentType.values())
            .map(BookMonographContentType::toString).collect(joining(DELIMITER)));
    }
}
