package no.sikt.nva.brage.migration.merger.publicationisntancemerger;

import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getNonNullValue;
import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getRange;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalLeader;

public final class JournalLeaderMerger {

    private JournalLeaderMerger() {
        super();
    }

    public static JournalLeader merge(JournalLeader journalLeader,
                                                             PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof JournalLeader newJournalLeader) {
            return new JournalLeader(getNonNullValue(journalLeader.getVolume(), newJournalLeader.getVolume()),
                                    getNonNullValue(journalLeader.getIssue(), newJournalLeader.getIssue()),
                                    getNonNullValue(journalLeader.getArticleNumber(), newJournalLeader.getArticleNumber()),
                                    getRange(journalLeader.getPages(), newJournalLeader.getPages()));
        } else {
            return journalLeader;
        }
    }
}
