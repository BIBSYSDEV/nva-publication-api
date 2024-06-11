package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import static no.sikt.nva.brage.migration.merger.publicationinstancemerger.PublicationInstanceMerger.getPages;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.report.ConferenceReport;

public final class ConferenceReportMerger {

    private ConferenceReportMerger() {
    }

    public static ConferenceReport merge(ConferenceReport conferenceReport, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ConferenceReport newConferenceReport) {
            return new ConferenceReport(getPages(conferenceReport.getPages(), newConferenceReport.getPages()));
        } else {
            return conferenceReport;
        }
    }
}
