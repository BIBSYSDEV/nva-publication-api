package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;

public final class ExhibitionCatalogMerger extends PublicationInstanceMerger<ExhibitionCatalog> {

    public ExhibitionCatalogMerger(ExhibitionCatalog exhibitionCatalog) {
        super(exhibitionCatalog);
    }

    @Override
    public ExhibitionCatalog merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof ExhibitionCatalog newExhibitionCatalog) {
            return new ExhibitionCatalog(getPages(this.publicationInstance.getPages(), newExhibitionCatalog.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
