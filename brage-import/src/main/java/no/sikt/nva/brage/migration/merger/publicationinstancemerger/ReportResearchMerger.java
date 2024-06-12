package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportResearch;

public final class ReportResearchMerger extends PublicationInstanceMerger<ReportResearch> {

    public ReportResearchMerger(ReportResearch reportResearch) {
        super(reportResearch);
    }

    @Override
    public ReportResearch merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportResearch newReportResearch) {
            return new ReportResearch(getPages(this.publicationInstance.getPages(), newReportResearch.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
