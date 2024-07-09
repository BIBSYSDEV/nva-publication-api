package no.unit.nva.model.contexttypes;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Revision;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;
import org.apache.commons.validator.routines.ISBNValidator;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Book implements BasicContext {

    public static final ISBNValidator ISBN_VALIDATOR = new ISBNValidator();
    public static final String JSON_PROPERTY_SERIES = "series";
    public static final String JSON_PROPERTY_SERIES_TITLE = "seriesTitle";
    public static final String JSON_PROPERTY_SERIES_NUMBER = "seriesNumber";
    public static final String JSON_PROPERTY_PUBLISHER = "publisher";
    public static final String JSON_PROPERTY_ISBN_LIST = "isbnList";
    public static final String JSON_PROPERTY_ADDITIONAL_IDENTIFIERS = "additionalIdentifiers";
    public static final String SPACES_AND_HYPHENS_REGEX = "[ -]";
    public static final String ISBN_SOURCE = "ISBN";
    public static final String JSON_PROPERTY_REVISION = "revision";

    @JsonProperty(JSON_PROPERTY_SERIES)
    private final BookSeries series;
    @JsonProperty(JSON_PROPERTY_SERIES_NUMBER)
    private final String seriesNumber;
    @JsonProperty(JSON_PROPERTY_PUBLISHER)
    private final PublishingHouse publisher;
    @JsonProperty(JSON_PROPERTY_ISBN_LIST)
    private final List<String> isbnList;
    @JsonProperty(JSON_PROPERTY_ADDITIONAL_IDENTIFIERS)
    private final Set<AdditionalIdentifier> additionalIdentifiers;
    private final Revision revision;

    public Book(@JsonProperty(JSON_PROPERTY_SERIES) BookSeries series,
                @JsonProperty(value = JSON_PROPERTY_SERIES_TITLE, access = WRITE_ONLY) String unconfirmedSeriesTitle,
                @JsonProperty(JSON_PROPERTY_SERIES_NUMBER) String seriesNumber,
                @JsonProperty(JSON_PROPERTY_PUBLISHER) PublishingHouse publisher,
                @JsonProperty(JSON_PROPERTY_ISBN_LIST) List<String> isbnList,
                @JsonProperty(JSON_PROPERTY_REVISION) Revision revision) throws InvalidUnconfirmedSeriesException {
        this(BookSeries.extractSeriesInformation(series, unconfirmedSeriesTitle),
             seriesNumber, publisher, isbnList, revision);
    }

    public Book(BookSeries series, String seriesNumber, PublishingHouse publisher, List<String> isbnList,
                Revision revision) {
        this.series = series;
        this.seriesNumber = seriesNumber;
        this.publisher = publisher;
        this.isbnList = extractValidIsbnList(isbnList);
        this.additionalIdentifiers = nonNull(isbnList) ? extractInvalidIsbn(isbnList) : Set.of();
        this.revision = revision;
    }

    private Set<AdditionalIdentifier> extractInvalidIsbn(List<String> isbnList) {
        return isbnList.stream()
                   .filter(isbn -> !this.isbnList.contains(isbn))
                   .map(isbn -> new AdditionalIdentifier(ISBN_SOURCE, isbn))
                   .collect(Collectors.toSet());
    }

    public Set<AdditionalIdentifier> getAdditionalIdentifiers() {
        return additionalIdentifiers;
    }

    public BookSeries getSeries() {
        return series;
    }

    public String getSeriesNumber() {
        return seriesNumber;
    }

    public PublishingHouse getPublisher() {
        return publisher;
    }

    public List<String> getIsbnList() {
        return nonNull(isbnList) ? isbnList : Collections.emptyList();
    }

    public Revision getRevision() {
        return revision;
    }

    public BookBuilder copy() {
        return new BookBuilder().withSeriesNumber(getSeriesNumber())
                   .withSeries(getSeries())
                   .withPublisher(getPublisher())
                   .withIsbnList(getIsbnList())
                   .withRevision(getRevision());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getSeries(), getSeriesNumber(), getPublisher(), getIsbnList(), additionalIdentifiers,
                            getRevision());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Book book = (Book) o;
        return Objects.equals(getSeries(), book.getSeries())
               && Objects.equals(getSeriesNumber(), book.getSeriesNumber())
               && Objects.equals(getPublisher(), book.getPublisher())
               && Objects.equals(getIsbnList(), book.getIsbnList())
               && Objects.equals(additionalIdentifiers, book.additionalIdentifiers)
               && Objects.equals(getRevision(), book.getRevision());
    }

    /**
     * Returns the ISBN list to the object after checking that the ISBNs are valid and removing ISBN-punctuation.
     *
     * @param isbnList List of ISBN candidates.
     * @return List of valid ISBN strings.
     */
    private List<String> extractValidIsbnList(List<String> isbnList) {
        if (isNull(isbnList) || isbnList.isEmpty()) {
            return Collections.emptyList();
        }
        return isbnList.stream()
                   .map(isbn -> isbn.replaceAll(SPACES_AND_HYPHENS_REGEX, ""))
                   .map(ISBN_VALIDATOR::validate)
                   .filter(Objects::nonNull)
                   .toList();
    }

    public static final class BookBuilder {

        private BookSeries series;
        private String seriesNumber;
        private PublishingHouse publisher;
        private List<String> isbnList;
        private Revision revision;

        public BookBuilder() {
        }

        public BookBuilder withSeries(BookSeries series) {
            this.series = series;
            return this;
        }

        public BookBuilder withSeriesNumber(String seriesNumber) {
            this.seriesNumber = seriesNumber;
            return this;
        }

        public BookBuilder withPublisher(PublishingHouse publisher) {
            this.publisher = publisher;
            return this;
        }

        public BookBuilder withIsbnList(List<String> isbnList) {
            this.isbnList = isbnList;
            return this;
        }

        public BookBuilder withRevision(Revision revision) {
            this.revision = revision;
            return this;
        }

        public Book build() {
            return new Book(series, seriesNumber, publisher, isbnList, revision);
        }
    }
}
