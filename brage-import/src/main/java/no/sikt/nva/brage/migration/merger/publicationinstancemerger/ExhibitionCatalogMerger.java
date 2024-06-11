package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;

public final class ExhibitionCatalogMerger extends PublicationInstanceMerger {

    private ExhibitionCatalogMerger() {
        super();
    }

    public static ExhibitionCatalog merge(ExhibitionCatalog exhibitionCatalog, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ExhibitionCatalog newExhibitionCatalog) {
            return new ExhibitionCatalog(getPages(exhibitionCatalog.getPages(), newExhibitionCatalog.getPages()));
        } else {
            return exhibitionCatalog;
        }
    }
}
