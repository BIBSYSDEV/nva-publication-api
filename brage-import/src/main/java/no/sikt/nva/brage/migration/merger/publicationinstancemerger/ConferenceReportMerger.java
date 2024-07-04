package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ConferenceReport;

public final class ConferenceReportMerger extends PublicationInstanceMerger<ConferenceReport> {

    public ConferenceReportMerger(ConferenceReport conferenceReport) {
        super(conferenceReport);
    }

    @Override
    public ConferenceReport merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ConferenceReport conferenceReport) {
            return new ConferenceReport(getPages(this.publicationInstance.getPages(), conferenceReport.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
