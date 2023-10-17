package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinMainCategory.isArt;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isBook;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isEvent;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isExhibition;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isJournal;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isMediaContribution;
import static no.unit.nva.cristin.mapper.CristinMainCategory.isReport;
import java.util.Objects;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedMainCategoryException;
import no.unit.nva.cristin.mapper.nva.exceptions.UnsupportedSecondaryCategoryException;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Pages;

public class PublicationInstanceBuilderImpl {

    public static final String ERROR_CRISTIN_OBJECT_IS_NULL = "CristinObject can not be null";

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
        } else if (isEvent(cristinObject)) {
            return new EventBuilder(cristinObject).build();
        } else if (isMediaContribution(cristinObject)) {
            return new MediaContributionBuilder(cristinObject).build();
        } else if (isArt(cristinObject)) {
            return new ArtBuilder(cristinObject).build();
        } else if (isExhibition(cristinObject)) {
            return new ExhibitionProductionBuilder(cristinObject).build();
        } else if (cristinObject.getMainCategory().isUnknownCategory()) {
            throw new UnsupportedMainCategoryException();
        } else if (cristinObject.getSecondaryCategory().isUnknownCategory()) {
            throw new UnsupportedSecondaryCategoryException();
        }
        throw new RuntimeException(ERROR_PARSING_MAIN_OR_SECONDARY_CATEGORIES);
    }
}
