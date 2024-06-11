package no.sikt.nva.brage.migration.merger.publicationInstanceMerger;

import static no.sikt.nva.brage.migration.merger.publicationInstanceMerger.PublicationInstanceMerger.getPages;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportWorkingPaper;

public final class ReportWorkingPaperMerger {

    private ReportWorkingPaperMerger() {
    }

    public static ReportWorkingPaper merge(ReportWorkingPaper reportWorkingPaper, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportWorkingPaper newReportWorkingPaper) {
            return new ReportWorkingPaper(getPages(reportWorkingPaper.getPages(), newReportWorkingPaper.getPages()));
        } else {
            return reportWorkingPaper;
        }
    }
}
