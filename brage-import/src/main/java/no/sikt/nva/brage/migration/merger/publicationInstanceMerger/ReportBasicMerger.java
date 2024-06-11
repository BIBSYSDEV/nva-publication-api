package no.sikt.nva.brage.migration.merger.publicationInstanceMerger;

import static no.sikt.nva.brage.migration.merger.publicationInstanceMerger.PublicationInstanceMerger.getPages;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportBasic;

public final class ReportBasicMerger {

    private ReportBasicMerger() {
    }

    public static ReportBasic merge(ReportBasic reportBasic, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportBasic newReportBasic) {
            return new ReportBasic(getPages(reportBasic.getPages(), newReportBasic.getPages()));
        } else {
            return reportBasic;
        }
    }
}
