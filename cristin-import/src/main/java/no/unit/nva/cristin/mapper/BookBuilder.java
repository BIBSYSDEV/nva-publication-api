package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_PEER_REVIEWED;
import static no.unit.nva.cristin.lambda.constants.HardcodedValues.HARDCODED_BOOK_TEXTBOOK_CONTENT;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isMonograph;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.BookMonograph;
import no.unit.nva.model.pages.Pages;

public class BookBuilder extends AbstractBookReportBuilder {

    public BookBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (CristinSecondaryCategory.isAnthology(getCristinObject())) {
            return createBookAnthology();
        } else if (isMonograph(getCristinObject())) {
            return createBookMonograph();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    @Override
    protected CristinMainCategory getExpectedType() {
        return CristinMainCategory.BOOK;
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
            .withContentType(getCristinObject().getSecondaryCategory().toBookMonographContentType())
            .withPeerReviewed(HARDCODED_BOOK_PEER_REVIEWED)
            .withPages(createMonographPages())
            .withTextbookContent(HARDCODED_BOOK_TEXTBOOK_CONTENT)
            .withPeerReviewed(getCristinObject().isPeerReviewed())
            .build();
    }

}
