package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import java.util.Objects;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;

public class PublicationInstanceBuilderImpl  {

    public static final String ERROR_CRISTIN_OBJECT_IS_NULL = "CristinObject can not be null";
    public static final String ERROR_PARSING_SECONDARY_CATEGORY = "Error parsing secondary category";
    public static final String ERROR_PARSING_MAIN_CATEGORY = "Error parsing main category";
    public static final String ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES = "Error parsing main or secondary "
                                                                            + "categories";

    private final CristinObject cristinObject;

    public PublicationInstanceBuilderImpl(CristinObject cristinObject) {
        Objects.requireNonNull(cristinObject, ERROR_CRISTIN_OBJECT_IS_NULL);
        this.cristinObject = cristinObject;
    }

    public PublicationInstance<? extends Pages> build() {
        if (isBook(cristinObject)) {
            return new BookBuilder(cristinObject).build();
        } else if (isReport(cristinObject)) {
            return new ReportBuilder(cristinObject).build();
        } else if (isJournal(cristinObject)) {
            return new JournalBuilder(cristinObject).build();
        } else if (isChapter(cristinObject)) {
            return new ChapterArticleBuilder(cristinObject).build();
        } else if (cristinObject.getMainCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_MAIN_CATEGORY);
        } else if (cristinObject.getSecondaryCategory().isUnknownCategory()) {
            throw new UnsupportedOperationException(ERROR_PARSING_SECONDARY_CATEGORY);
        }
        throw new RuntimeException(ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES);
    }
}
