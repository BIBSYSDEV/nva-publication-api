package no.unit.nva.publication.doi.dto;

import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

public class PublicationDate {

    public static final String YEAR_JSON_POINTER = "/publication_date/year";
    public static final String MONTH_JSON_POINTER = "/publication_date/month";
    public static final String DAY_JSON_POINTER = "/publication_date/day";

    private final String year;
    private final String month;
    private final String day;

    /**
     * Constructor for basic deserialization of PublicationDate.
     * @param year String representing year.
     * @param month String representing month.
     * @param day String representing day.
     */
    @JsonCreator
    public PublicationDate(@JsonProperty("year") String year,
                           @JsonProperty("month") String month,
                           @JsonProperty("day") String day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    /**
     * Constructor for PublicationDate.
     * @param doiPublicationDto JsonNode representation of a doiPublicationDto
     */
    public PublicationDate(JsonNode doiPublicationDto) {
        this.year = extractYear(doiPublicationDto);
        this.month = extractMonth(doiPublicationDto);
        this.day = extractDay(doiPublicationDto);
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

    @JsonIgnore
    public boolean isPopulated() {
        return isNotNullOrEmpty(year) || isNotNullOrEmpty(month) || isNotNullOrEmpty(day);
    }

    private String extractDay(JsonNode record) {
        return textFromNode(record, DAY_JSON_POINTER);
    }

    private boolean isNotNullOrEmpty(String string) {
        return nonNull(string) && !string.isEmpty();
    }

    private String extractMonth(JsonNode record) {
        return textFromNode(record, MONTH_JSON_POINTER);
    }

    private String extractYear(JsonNode record) {
        return textFromNode(record, YEAR_JSON_POINTER);
    }

    private String textFromNode(JsonNode jsonNode, String jsonPointer) {
        JsonNode json = jsonNode.at(jsonPointer);
        return isPopulatedJsonPointer(json) ? json.asText() : null;
    }

    private boolean isPopulatedJsonPointer(JsonNode json) {
        return !json.isNull() && !json.asText().isBlank();
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
}

