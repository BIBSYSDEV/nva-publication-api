package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalLeader;

public final class JournalLeaderMerger extends PublicationInstanceMerger<JournalLeader> {

    public JournalLeaderMerger(JournalLeader journalLeader) {
        super(journalLeader);
    }

    @Override
    public JournalLeader merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof JournalLeader journalLeader) {
            return new JournalLeader(getNonNullValue(this.publicationInstance.getVolume(), journalLeader.getVolume()),
                                    getNonNullValue(this.publicationInstance.getIssue(), journalLeader.getIssue()),
                                    getNonNullValue(this.publicationInstance.getArticleNumber(),
                                                    journalLeader.getArticleNumber()),
                                    getRange(this.publicationInstance.getPages(), journalLeader.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
