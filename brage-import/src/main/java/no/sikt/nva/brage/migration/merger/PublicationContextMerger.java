package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import java.util.List;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import nva.commons.core.JacocoGenerated;

public class PublicationContextMerger {

    @JacocoGenerated
    public PublicationContextMerger() {
    }

    public static List<String> getIsbnList(List<String> existingList, List<String> newList) {
        return nonNull(existingList) && !existingList.isEmpty() ? existingList : newList;
    }

    public static BookSeries getSeries(BookSeries oldSeries, BookSeries newSeries) {
        if (nonNull(oldSeries) && oldSeries instanceof Series series) {
            return series;
        }
        if (nonNull(newSeries) && newSeries instanceof Series series) {
            return series;
        }
        if (nonNull(oldSeries) && oldSeries instanceof UnconfirmedSeries unconfirmedSeries) {
            return unconfirmedSeries;
        } else {
            return newSeries;
        }
    }

    public static String getSeriesNumber(String oldSeriesNumber, String newSeriesNumber) {
        return nonNull(oldSeriesNumber) ? oldSeriesNumber : newSeriesNumber;
    }

    public static PublishingHouse getPublisher(PublishingHouse oldPublisher, PublishingHouse newPublisher) {
        if (nonNull(oldPublisher) && oldPublisher instanceof Publisher publisher) {
            return publisher;
        }
        if (nonNull(newPublisher) && newPublisher instanceof Publisher publisher) {
            return publisher;
        }
        if (nonNull(oldPublisher) && oldPublisher instanceof UnconfirmedPublisher unconfirmedPublisher) {
            return unconfirmedPublisher;
        } else {
            return newPublisher;
        }
    }

    public static String getNonNullValue(String oldValue, String newValue) {
        return nonNull(oldValue) ? oldValue : newValue;
    }
}
