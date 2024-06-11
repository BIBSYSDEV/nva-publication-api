package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalIssue;

public final class JournalIssueMerger extends PublicationInstanceMerger<JournalIssue> {

    public JournalIssueMerger(JournalIssue journalIssue) {
        super(journalIssue);
    }

    @Override
    public JournalIssue merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof JournalIssue newJournalIssue) {
            return new JournalIssue(getNonNullValue(this.publicationInstance.getVolume(), newJournalIssue.getVolume()),
                                    getNonNullValue(this.publicationInstance.getIssue(), newJournalIssue.getIssue()),
                                    getNonNullValue(this.publicationInstance.getArticleNumber(), newJournalIssue.getArticleNumber()),
                                    getRange(this.publicationInstance.getPages(), newJournalIssue.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
