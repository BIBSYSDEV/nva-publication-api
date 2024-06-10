package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import static no.sikt.nva.brage.migration.merger.PublicationContextMerger.getIsbnList;
import static no.sikt.nva.brage.migration.merger.PublicationContextMerger.getPublisher;
import static no.sikt.nva.brage.migration.merger.PublicationContextMerger.getSeries;
import static no.sikt.nva.brage.migration.merger.PublicationContextMerger.getSeriesNumber;
import no.unit.nva.model.Revision;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;

public final class BookMerger {

    private BookMerger() {
    }

    public static Book merge(Book book, PublicationContext publicationContext) {
        if (publicationContext instanceof Book newBook) {
            return new Book.BookBuilder().withIsbnList(getIsbnList(book.getIsbnList(), newBook.getIsbnList()))
                       .withSeries(getSeries(book.getSeries(), newBook.getSeries()))
                       .withPublisher(getPublisher(book.getPublisher(), newBook.getPublisher()))
                       .withSeriesNumber(getSeriesNumber(book.getSeriesNumber(), newBook.getSeriesNumber()))
                       .withRevision(getRevision(book, newBook))
                       .build();
        } else {
            return book;
        }
    }

    private static Revision getRevision(Book oldBook, Book newBook) {
        return nonNull(oldBook.getRevision()) ? oldBook.getRevision() : newBook.getRevision();
    }
}
