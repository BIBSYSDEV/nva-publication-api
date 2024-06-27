package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import no.sikt.nva.brage.migration.record.Record;
import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.PublicationContext;
import nva.commons.core.JacocoGenerated;

public class GeographicalContentMerger extends PublicationContextMerger{

    @JacocoGenerated
    public GeographicalContentMerger(Record record) {
        super(record);
    }

    public PublicationContext merge(GeographicalContent geographicalContent,
                                           PublicationContext publicationContext) {
        if (publicationContext instanceof GeographicalContent newGeographicalContent) {
            return new GeographicalContent(getPublisher(
                geographicalContent.getPublisher(),
                newGeographicalContent.getPublisher()));
        } else {
            return geographicalContent;
        }
    }
}
