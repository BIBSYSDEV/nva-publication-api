package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.Textbook;

public final class TextbookMerger extends PublicationInstanceMerger<Textbook> {

    public TextbookMerger(Textbook textbook) {
        super(textbook);
    }

    @Override
    public Textbook merge(PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof Textbook newTextBook) {
            return new Textbook(getPages(this.publicationInstance.getPages(), newTextBook.getPages()));
        } else {
            return this.publicationInstance;
        }
    }
}
