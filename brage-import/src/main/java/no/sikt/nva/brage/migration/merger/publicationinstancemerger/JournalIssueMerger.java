package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalIssue;

public final class JournalIssueMerger extends PublicationInstanceMerger {

    private JournalIssueMerger() {
        super();
    }

    public static JournalIssue merge(JournalIssue journalIssue, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof JournalIssue newJournalIssue) {
            return new JournalIssue(getNonNullValue(journalIssue.getVolume(), newJournalIssue.getVolume()),
                                    getNonNullValue(journalIssue.getIssue(), newJournalIssue.getIssue()),
                                    getNonNullValue(journalIssue.getArticleNumber(), newJournalIssue.getArticleNumber()),
                                    getRange(journalIssue.getPages(), newJournalIssue.getPages()));
        } else {
            return journalIssue;
        }
    }
}
