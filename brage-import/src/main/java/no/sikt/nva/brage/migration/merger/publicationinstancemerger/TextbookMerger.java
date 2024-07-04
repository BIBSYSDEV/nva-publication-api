package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.Textbook;

public final class TextbookMerger extends PublicationInstanceMerger<Textbook> {

    public TextbookMerger(Textbook textbook) {
        super(textbook);
    }

    @Override
    public Textbook merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof Textbook textbook) {
            return new Textbook(getPages(this.publicationInstance.getPages(), textbook.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
