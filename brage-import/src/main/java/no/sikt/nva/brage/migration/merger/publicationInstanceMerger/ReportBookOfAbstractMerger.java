package no.sikt.nva.brage.migration.merger.publicationInstanceMerger;

import static no.sikt.nva.brage.migration.merger.publicationInstanceMerger.PublicationInstanceMerger.getPages;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;

public final class ReportBookOfAbstractMerger {

    private ReportBookOfAbstractMerger() {
    }

    public static ReportBookOfAbstract merge(ReportBookOfAbstract reportBookOfAbstract, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportBookOfAbstract newReportBookOfAbstract) {
            return new ReportBookOfAbstract(getPages(reportBookOfAbstract.getPages(), newReportBookOfAbstract.getPages()));
        } else {
            return reportBookOfAbstract;
        }
    }
}
