package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import static java.util.Objects.nonNull;
import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.contexttypes.Journal;
import no.unit.nva.model.contexttypes.Periodical;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.UnconfirmedJournal;
import no.unit.nva.model.exceptions.InvalidIssnException;
import nva.commons.core.JacocoGenerated;

public final class JournalMerger extends PublicationContextMerger {

    @JacocoGenerated
    private JournalMerger(Record record) {
        super(record);
    }

    public static PublicationContext merge(Periodical journal, PublicationContext publicationContext)
        throws InvalidIssnException {
        if (publicationContext instanceof Periodical newJournal) {
            return getJournal(journal, newJournal);
        } else {
            return journal;
        }
    }

    private static PublicationContext getJournal(Periodical oldJournal, Periodical newJournal)
        throws InvalidIssnException {
        if (nonNull(oldJournal) && oldJournal instanceof Journal journal) {
            return journal;
        }
        if (nonNull(newJournal) && newJournal instanceof Journal journal) {
            return journal;
        }
        if (oldJournal instanceof UnconfirmedJournal oldUnconfirmedJournal
            && newJournal instanceof UnconfirmedJournal newUnconfirmedJournal) {
            return new UnconfirmedJournal(
                getNonNullValue(oldUnconfirmedJournal.getTitle(), newUnconfirmedJournal.getTitle()),
                getNonNullValue(oldUnconfirmedJournal.getPrintIssn(), newUnconfirmedJournal.getPrintIssn()),
                getNonNullValue(oldUnconfirmedJournal.getOnlineIssn(), newUnconfirmedJournal.getOnlineIssn()));
        } else {
            return oldJournal;
        }
    }
}

