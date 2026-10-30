package no.unit.nva.model.contexttypes;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;

public class Report extends Book {

  @JsonCreator
  public Report(
      @JsonProperty(JSON_PROPERTY_SERIES) BookSeries series,
      @JsonProperty(JSON_PROPERTY_SERIES_TITLE) String seriesTitle,
      @JsonProperty(JSON_PROPERTY_SERIES_NUMBER) String seriesNumber,
      @JsonProperty(JSON_PROPERTY_PUBLISHER) PublishingHouse publisher,
      @JsonProperty(JSON_PROPERTY_ISBN_LIST) List<String> isbnList)
      throws InvalidUnconfirmedSeriesException {
    super(series, seriesTitle, seriesNumber, publisher, isbnList, null);
  }

  private Report(Builder builder) throws InvalidUnconfirmedSeriesException {
    this(builder.series, null, builder.seriesNumber, builder.publisher, builder.isbnList);
  }

  @Override
  public Report withSeries(BookSeries series) {
    return rebuild(series, getPublisher());
  }

  @Override
  public Report withPublisher(PublishingHouse publisher) {
    return rebuild(getSeries(), publisher);
  }

  private Report rebuild(BookSeries series, PublishingHouse publisher) {
    return attempt(
            () ->
                new Builder()
                    .withSeries(series)
                    .withSeriesNumber(getSeriesNumber())
                    .withPublisher(publisher)
                    .withIsbnList(getIsbnList())
                    .build())
        .orElseThrow();
  }

  public static final class Builder {

    private BookSeries series;
    private String seriesNumber;
    private PublishingHouse publisher;
    private List<String> isbnList;

    public Builder() {}

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

    public Report build() throws InvalidIssnException, InvalidUnconfirmedSeriesException {
      return new Report(this);
    }
  }
}
