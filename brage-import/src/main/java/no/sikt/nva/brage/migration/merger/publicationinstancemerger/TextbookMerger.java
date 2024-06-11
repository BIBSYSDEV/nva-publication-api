package no.sikt.nva.brage.migration.merger.publicationinstancemerger;

import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.Textbook;

public final class TextbookMerger extends PublicationInstanceMerger{

    private TextbookMerger() {
        super();
    }

    public static Textbook merge(Textbook textbook, PublicationInstance<?> publicationInstance) {
        if (publicationInstance instanceof Textbook newTextBook) {
            return new Textbook(getPages(textbook.getPages(), newTextBook.getPages()));
        } else {
            return textbook;
        }
    }
}
