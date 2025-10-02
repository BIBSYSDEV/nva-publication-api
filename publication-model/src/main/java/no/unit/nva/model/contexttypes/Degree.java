package no.unit.nva.model.contexttypes;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Objects;
import no.unit.nva.model.Course;
import no.unit.nva.model.exceptions.InvalidIsbnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Degree extends Book {

    public static final String JSON_PROPERTY_COURSE_CODE = "courseCode";
    public static final String JSON_PROPERTY_COURSE = "course";
    private final Course course;

    @JsonCreator
    public Degree(@JsonProperty(JSON_PROPERTY_SERIES) BookSeries series,
                  @JsonProperty(value = JSON_PROPERTY_SERIES_TITLE, access = WRITE_ONLY) String unconfirmedSeriesTitle,
                  @JsonProperty(JSON_PROPERTY_SERIES_NUMBER) String seriesNumber,
                  @JsonProperty(JSON_PROPERTY_PUBLISHER) PublishingHouse publisher,
                  @JsonProperty(JSON_PROPERTY_ISBN_LIST) List<String> isbnList,
                  @JsonProperty(JSON_PROPERTY_COURSE) @JsonAlias(JSON_PROPERTY_COURSE_CODE) Course course)
        throws InvalidUnconfirmedSeriesException {
        super(series, unconfirmedSeriesTitle, seriesNumber, publisher, isbnList, null);
        this.course = course;
    }

    private Degree(Builder builder, Course courseCode) throws InvalidUnconfirmedSeriesException {
        super(builder.series, null, builder.seriesNumber, builder.publisher, builder.isbnList, null);
        this.course = courseCode;
    }

    public Course getCourse() {
        return course;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), course);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Degree degree = (Degree) o;
        return Objects.equals(course, degree.course);
    }

    public static final class Builder {

        private BookSeries series;
        private String seriesNumber;
        private PublishingHouse publisher;
        private List<String> isbnList;
        private Course course;

        public Builder() {
        }

        public Builder withSeries(BookSeries series) {
            this.series = series;
            return this;
        }

        public Builder withSeriesNumber(String seriesNumber) {
            this.seriesNumber = seriesNumber;
            return this;
        }

        public Builder withPublisher(PublishingHouse publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder withIsbnList(List<String> isbnList) {
            this.isbnList = isbnList;
            return this;
        }

        public Builder withCourse(Course course) {
            this.course = course;
            return this;
        }

        public Degree build() throws InvalidIsbnException, InvalidUnconfirmedSeriesException {
            return new Degree(this, course);
        }
    }
}
