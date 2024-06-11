package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;

public final class NonFictionMonographMerger extends PublicationInstanceMerger{

    private NonFictionMonographMerger() {
        super();
    }

    public static NonFictionMonograph merge(NonFictionMonograph nonFictionMonograph, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof NonFictionMonograph newNonFictionMonograph) {
            return new NonFictionMonograph(getPages(nonFictionMonograph.getPages(), newNonFictionMonograph.getPages()));
        } else {
            return nonFictionMonograph;
        }
    }
}
