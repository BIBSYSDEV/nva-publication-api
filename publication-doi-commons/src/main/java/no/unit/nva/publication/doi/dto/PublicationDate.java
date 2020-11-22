package no.unit.nva.publication.doi.dto;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.doi.JsonPointerUtils.textFromNode;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

public class PublicationDate extends Validatable {

    public static final JsonPointer YEAR_JSON_POINTER = JsonPointer.compile("/date/m/year/s");
    public static final JsonPointer MONTH_JSON_POINTER = JsonPointer.compile("/date/m/month/s");
    public static final JsonPointer DAY_JSON_POINTER = JsonPointer.compile("/date/m/day/s");

    private final String year;
    private final String month;
    private final String day;

    /**
     * Constructor for basic deserialization of PublicationDate.
     *
     * @param year  String representing year.
     * @param month String representing month.
     * @param day   String representing day.
     */
    @JsonCreator
    public PublicationDate(@JsonProperty("year") String year,
                           @JsonProperty("month") String month,
                           @JsonProperty("day") String day) {
        super();
        this.year = year;
        this.month = month;
        this.day = day;
    }

    /**
     * Constructor for PublicationDate.
     *
     * @param doiPublicationDto JsonNode representation of a doiPublicationDto
     */
    public static PublicationDate fromJsonNode(JsonNode doiPublicationDto) {
        PublicationDate publcationDate = new PublicationDate(extractYear(doiPublicationDto),
            extractMonth(doiPublicationDto),
            extractDay(doiPublicationDto));
        publcationDate.validate();
        return publcationDate;
    }

    public String getYear() {
        return year;
    }

    public String getMonth() {
        return month;
    }

    public String getDay() {
        return day;
    }

    @Override
    public void validate() {
        requireFieldIsNotNull(year, "PublicationDate.year");
    }

    @JsonIgnore
    public boolean isPopulated() {
        return isNotNullOrBlank(year) || isNotNullOrBlank(month) || isNotNullOrBlank(day);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PublicationDate)) {
            return false;
        }
        PublicationDate date = (PublicationDate) o;
        return Objects.equals(getYear(), date.getYear())
            && Objects.equals(getMonth(), date.getMonth())
            && Objects.equals(getDay(), date.getDay());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getYear(), getMonth(), getDay());
    }

    private static String extractDay(JsonNode record) {
        return nonNull(record) ? textFromNode(record, DAY_JSON_POINTER) : null;
    }

    private static String extractMonth(JsonNode record) {
        return nonNull(record) ? textFromNode(record, MONTH_JSON_POINTER) : null;
    }

    private static String extractYear(JsonNode record) {
        return nonNull(record) ? textFromNode(record, YEAR_JSON_POINTER) : null;
    }

    private boolean isNotNullOrBlank(String string) {
        return nonNull(string) && !string.isBlank();
    }
}

