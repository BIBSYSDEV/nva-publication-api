package no.unit.nva.model.contexttypes;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "Series", value = Series.class),
    @JsonSubTypes.Type(name = "UnconfirmedSeries", value = UnconfirmedSeries.class)
})
public interface BookSeries {

    @JsonIgnore
    boolean isConfirmed();

    static BookSeries extractSeriesInformation(BookSeries series, String unconfirmedSeriesTitle)
            throws InvalidUnconfirmedSeriesException {

        if (nonNull(series) && series.isConfirmed()) {
            return series;
        }

        validateUnconfirmedSeries(series, unconfirmedSeriesTitle);

        if (nonNull(unconfirmedSeriesTitle) && isNull(series)) {
            return  UnconfirmedSeries.fromTitle(unconfirmedSeriesTitle);
        } else {
            return series;
        }
    }

    private static void validateUnconfirmedSeries(BookSeries series, String unconfirmedSeriesTitle)
            throws InvalidUnconfirmedSeriesException {
        if (hasSeriesStringAndSeriesObject(series, unconfirmedSeriesTitle)
                && hasUnmatchedSeriesStringValues(series, unconfirmedSeriesTitle)) {
            throw new InvalidUnconfirmedSeriesException();
        }
    }

    private static boolean hasUnmatchedSeriesStringValues(BookSeries series, String unconfirmedSeriesTitle) {
        return !series.isConfirmed()
                && !((UnconfirmedSeries) series).getTitle().equals(unconfirmedSeriesTitle);
    }

    private static boolean hasSeriesStringAndSeriesObject(BookSeries series, String unconfirmedSeriesTitle) {
        return nonNull(series) && nonNull(unconfirmedSeriesTitle);
    }
}
