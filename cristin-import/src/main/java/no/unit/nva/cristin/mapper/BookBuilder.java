package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_PEER_REVIEWED;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_TEXTBOOK_CONTENT;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isMonograph;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.pages.Pages;

public class BookBuilder extends AbstractBookReportBuilder {

    public static final String MAIN_CATEGORY_BOOK = "Book (BOK)";

    private final CristinObject cristinObject;

    public BookBuilder(CristinObject cristinObject) {
        super();
        if (!isBook(cristinObject)) {
            throw new IllegalStateException(
                    String.format(ERROR_NOT_CORRECT_TYPE, this.getClass().getSimpleName(), MAIN_CATEGORY_BOOK)
            );
        }
        this.cristinObject = cristinObject;
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (CristinSecondaryCategory.isAnthology(cristinObject)) {
            return createBookAnthology();
        } else if (isMonograph(cristinObject)) {
            return createBookMonograph();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    @Override
    protected CristinObject getCristinObject() {
        return cristinObject;
    }

    private BookAnthology createBookAnthology() {
        return new BookAnthology.Builder()
            .withPeerReviewed(HARDCODED_BOOK_PEER_REVIEWED)
            .withPages(createMonographPages())
            .withTextbookContent(HARDCODED_BOOK_TEXTBOOK_CONTENT)
            .build();
    }

    private BookMonograph createBookMonograph() {
        return new BookMonograph.Builder()
            .withPeerReviewed(HARDCODED_BOOK_PEER_REVIEWED)
            .withPages(createMonographPages())
            .withTextbookContent(HARDCODED_BOOK_TEXTBOOK_CONTENT)
            .build();
    }
}
