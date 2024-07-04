package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;

public final class NonFictionMonographMerger extends PublicationInstanceMerger<NonFictionMonograph> {

    public NonFictionMonographMerger(NonFictionMonograph nonFictionMonograph) {
        super(nonFictionMonograph);
    }

    @Override
    public NonFictionMonograph merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof NonFictionMonograph newNonFictionMonograph) {
            return new NonFictionMonograph(getPages(this.publicationInstance.getPages(),
                                                    newNonFictionMonograph.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
