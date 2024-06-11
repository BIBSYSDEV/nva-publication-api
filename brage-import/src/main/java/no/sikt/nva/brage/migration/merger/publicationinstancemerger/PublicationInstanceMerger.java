package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static java.util.Objects.nonNull;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.pages.MonographPages;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

public class PublicationInstanceMerger {

    @JacocoGenerated
    protected PublicationInstanceMerger() {
    }

    public static MonographPages getPages(MonographPages pages, MonographPages bragePages) {
        return nonNull(pages) ? pages : bragePages;
    }

    public static Range getRange(Range oldRange, Range newRange) {
        return nonNull(oldRange) ? oldRange : newRange;
    }

    public static PublicationDate getDate(PublicationDate submittedDate, PublicationDate brageDate) {
        return nonNull(submittedDate) ? submittedDate : brageDate;
    }

    public static String getNonNullValue(String oldValue, String newValue) {
        return nonNull(oldValue) ? oldValue : newValue;
    }
}
