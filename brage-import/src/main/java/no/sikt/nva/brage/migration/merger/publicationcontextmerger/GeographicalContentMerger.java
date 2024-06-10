package no.sikt.nva.brage.migration.merger.publicationcontextmerger;

import no.unit.nva.model.contexttypes.GeographicalContent;
import no.unit.nva.model.contexttypes.PublicationContext;

public final class GeographicalContentMerger extends PublicationContextMerger{

    private GeographicalContentMerger() {
    }

    public static PublicationContext merge(GeographicalContent geographicalContent,
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
