package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportBookOfAbstract;

public final class ReportBookOfAbstractMerger extends PublicationInstanceMerger<ReportBookOfAbstract> {

    public ReportBookOfAbstractMerger(ReportBookOfAbstract reportBookOfAbstract) {
        super(reportBookOfAbstract);
    }

    @Override
    public ReportBookOfAbstract merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportBookOfAbstract newReportBookOfAbstract) {
            return new ReportBookOfAbstract(getPages(this.publicationInstance.getPages(), newReportBookOfAbstract.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
