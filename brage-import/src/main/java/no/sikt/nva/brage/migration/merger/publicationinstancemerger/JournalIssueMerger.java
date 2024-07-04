package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalIssue;

public final class JournalIssueMerger extends PublicationInstanceMerger<JournalIssue> {

    public JournalIssueMerger(JournalIssue journalIssue) {
        super(journalIssue);
    }

    @Override
    public JournalIssue merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof JournalIssue journalIssue) {
            return new JournalIssue(getNonNullValue(this.publicationInstance.getVolume(), journalIssue.getVolume()),
                                    getNonNullValue(this.publicationInstance.getIssue(), journalIssue.getIssue()),
                                    getNonNullValue(this.publicationInstance.getArticleNumber(),
                                                    journalIssue.getArticleNumber()),
                                    getRange(this.publicationInstance.getPages(), journalIssue.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
