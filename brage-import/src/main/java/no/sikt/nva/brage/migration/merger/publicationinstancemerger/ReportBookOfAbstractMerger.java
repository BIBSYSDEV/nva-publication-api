package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;

public final class ReportBookOfAbstractMerger extends PublicationInstanceMerger {

    private ReportBookOfAbstractMerger() {
        super();
    }

    public static ReportBookOfAbstract merge(ReportBookOfAbstract reportBookOfAbstract, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportBookOfAbstract newReportBookOfAbstract) {
            return new ReportBookOfAbstract(getPages(reportBookOfAbstract.getPages(), newReportBookOfAbstract.getPages()));
        } else {
            return reportBookOfAbstract;
        }
    }
}
