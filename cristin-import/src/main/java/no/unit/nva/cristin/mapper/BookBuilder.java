package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isMonograph;
import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.book.AcademicMonograph;
import no.unit.nva.model.instancetypes.book.BookAnthology;
import no.unit.nva.model.instancetypes.book.Encyclopedia;
import no.unit.nva.model.instancetypes.book.ExhibitionCatalog;
import no.unit.nva.model.instancetypes.book.NonFictionMonograph;
import no.unit.nva.model.instancetypes.book.PopularScienceMonograph;
import no.unit.nva.model.instancetypes.book.Textbook;
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
            return createMonograph();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    private PublicationInstance<? extends Pages> createMonograph() {

        var secondaryCategory = getCristinObject().getSecondaryCategory();
        if (CristinSecondaryCategory.MONOGRAPH.equals(secondaryCategory)) {
            return new AcademicMonograph(createMonographPages());
        } else if (CristinSecondaryCategory.TEXTBOOK.equals(secondaryCategory)) {
            return new Textbook(createMonographPages());
        } else if (CristinSecondaryCategory.NON_FICTION_BOOK.equals(secondaryCategory)) {
            return new NonFictionMonograph(createMonographPages());
        } else if (CristinSecondaryCategory.ENCYCLOPEDIA.equals(secondaryCategory)) {
            return new Encyclopedia(createMonographPages());
        } else if (CristinSecondaryCategory.POPULAR_BOOK.equals(secondaryCategory)) {
            return new PopularScienceMonograph(createMonographPages());
        } else if (CristinSecondaryCategory.REFERENCE_MATERIAL.equals(secondaryCategory)) {
            return new Encyclopedia(createMonographPages());
        } else if (CristinSecondaryCategory.EXHIBITION_CATALOG.equals(secondaryCategory)) {
            return new ExhibitionCatalog(createMonographPages());
        } else if (CristinSecondaryCategory.ACADEMIC_COMMENTARY.equals(secondaryCategory)) {
            return new AcademicMonograph(createMonographPages());
        } else {
            throw new UnsupportedOperationException("Unknown monograph type");
        }
    }

    @Override
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.BOOK);
    }

    private BookAnthology createBookAnthology() {
        return new BookAnthology(createMonographPages());
    }
}
