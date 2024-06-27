package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.contexttypes.PublicationContext;
import no.unit.nva.model.contexttypes.Report;
import no.unit.nva.model.exceptions.InvalidIssnException;
import no.unit.nva.model.exceptions.InvalidUnconfirmedSeriesException;
import nva.commons.core.JacocoGenerated;

public class ReportMerger extends PublicationContextMerger {

    @JacocoGenerated
    public ReportMerger(Record record) {
        super(record);
    }

    public Report merge(Report report, PublicationContext publicationContext)
        throws InvalidIssnException, InvalidUnconfirmedSeriesException {
        if (publicationContext instanceof Report newReport) {
            return new Report.Builder().withSeriesNumber(
                    getNonNullValue(report.getSeriesNumber(), newReport.getSeriesNumber()))
                       .withIsbnList(getIsbnList(report.getIsbnList(), newReport.getIsbnList()))
                       .withPublisher(getPublisher(report.getPublisher(), newReport.getPublisher()))
                       .withSeries(getSeries(report.getSeries(), newReport.getSeries()))
                       .build();
        } else {
            return report;
        }
    }
}
