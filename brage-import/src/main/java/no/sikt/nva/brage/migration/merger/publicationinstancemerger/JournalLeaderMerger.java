package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.journal.JournalLeader;

public final class JournalLeaderMerger extends PublicationInstanceMerger<JournalLeader> {

    public JournalLeaderMerger(JournalLeader journalLeader) {
        super(journalLeader);
    }

    @Override
    public JournalLeader merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof JournalLeader newJournalLeader) {
            return new JournalLeader(getNonNullValue(this.publicationInstance.getVolume(), newJournalLeader.getVolume()),
                                    getNonNullValue(this.publicationInstance.getIssue(), newJournalLeader.getIssue()),
                                    getNonNullValue(this.publicationInstance.getArticleNumber(), newJournalLeader.getArticleNumber()),
                                    getRange(this.publicationInstance.getPages(), newJournalLeader.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
