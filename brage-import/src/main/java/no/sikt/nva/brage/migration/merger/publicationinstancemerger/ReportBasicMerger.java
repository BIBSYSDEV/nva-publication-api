package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportBasic;

public final class ReportBasicMerger extends PublicationInstanceMerger<ReportBasic> {

    public ReportBasicMerger(ReportBasic reportBasic) {
        super(reportBasic);
    }

    @Override
    public ReportBasic merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportBasic newReportBasic) {
            return new ReportBasic(getPages(this.publicationInstance.getPages(), newReportBasic.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}