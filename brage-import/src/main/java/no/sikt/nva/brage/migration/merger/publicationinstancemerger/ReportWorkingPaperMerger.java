package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;

public final class ReportWorkingPaperMerger extends PublicationInstanceMerger<ReportWorkingPaper> {

    public ReportWorkingPaperMerger(ReportWorkingPaper reportWorkingPaper) {
        super(reportWorkingPaper);
    }

    @Override
    public ReportWorkingPaper merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportWorkingPaper ReportWorkingPaper) {
            return new ReportWorkingPaper(getPages(this.publicationInstance.getPages(), ReportWorkingPaper.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
