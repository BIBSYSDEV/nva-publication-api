package no.sikt.nva.brage.migration.merger;

import static java.util.Objects.nonNull;
import no.unit.nva.model.Revision;
import no.unit.nva.model.contexttypes.Book;
import no.unit.nva.model.contexttypes.PublicationContext;

public final class BookMerger extends PublicationContextMerger {

    private BookMerger() {
    }

    public static Book merge(Book book, PublicationContext publicationContext) {
        if (publicationContext instanceof Book newBook) {
            return new Book.BookBuilder().withIsbnList(getIsbnList(book.getIsbnList(), newBook.getIsbnList()))
                       .withSeries(getSeries(book.getSeries(), newBook.getSeries()))
                       .withPublisher(getPublisher(book.getPublisher(), newBook.getPublisher()))
                       .withSeriesNumber(getNonNullValue(book.getSeriesNumber(), newBook.getSeriesNumber()))
                       .withRevision(getRevision(book, newBook))
                       .withIsbnList(getIsbnList(book.getIsbnList(), newBook.getIsbnList()))
                       .build();
        } else {
            return book;
        }
    }

    private static Revision getRevision(Book oldBook, Book newBook) {
        return nonNull(oldBook.getRevision()) ? oldBook.getRevision() : newBook.getRevision();
    }
}
