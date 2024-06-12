package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;

public final class ReportWorkingPaperMerger extends PublicationInstanceMerger<ReportWorkingPaper> {

    public ReportWorkingPaperMerger(ReportWorkingPaper reportWorkingPaper) {
        super(reportWorkingPaper);
    }

    @Override
    public ReportWorkingPaper merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportWorkingPaper newReportWorkingPaper) {
            return new ReportWorkingPaper(getPages(this.publicationInstance.getPages(), newReportWorkingPaper.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
