package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static java.util.Objects.nonNull;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

public class PublicationInstanceMerger {

    @JacocoGenerated
    PublicationInstanceMerger() {
    }

    public static MonographPages getPages(MonographPages pages, MonographPages bragePages) {
        return nonNull(pages) ? pages : bragePages;
    }

    public static PublicationDate getDate(PublicationDate submittedDate, PublicationDate brageDate) {
        return nonNull(submittedDate) ? submittedDate : brageDate;
    }
}
