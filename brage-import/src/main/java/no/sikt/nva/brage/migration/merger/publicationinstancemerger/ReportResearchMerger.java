package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getPages;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ReportResearch;

public final class ReportResearchMerger {

    private ReportResearchMerger() {
        super();
    }

    public static ReportResearch merge(ReportResearch reportResearch, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ReportResearch newReportResearch) {
            return new ReportResearch(getPages(reportResearch.getPages(), newReportResearch.getPages()));
        } else {
            return reportResearch;
        }
    }
}
