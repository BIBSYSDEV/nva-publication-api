package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import static java.util.Objects.nonNull;
import java.util.List;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.contexttypes.BookSeries;
import no.unit.nva.model.contexttypes.Publisher;
import no.unit.nva.model.contexttypes.PublishingHouse;
import no.unit.nva.model.contexttypes.Series;
import no.unit.nva.model.contexttypes.UnconfirmedPublisher;
import no.unit.nva.model.contexttypes.UnconfirmedSeries;
import no.unit.nva.model.time.Time;
import nva.commons.core.JacocoGenerated;

public class PublicationContextMerger {

    private final boolean shouldPrioritizeBragePublisher;

    @JacocoGenerated
    public PublicationContextMerger(Record record) {
        this.shouldPrioritizeBragePublisher = checkPrioritizedPublisher(record);
    }

    private static boolean checkPrioritizedPublisher(Record record) {
        return record.getPrioritizedProperties().contains("publisher");
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

    public PublishingHouse getPublisher(PublishingHouse oldPublisher, PublishingHouse newPublisher) {
        if (shouldPrioritizeBragePublisher) {
            return newPublisher;
        }
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

    public static Time getTime(Time oldTime, Time newTime) {
        return nonNull(oldTime) ? oldTime : newTime;
    }
}
