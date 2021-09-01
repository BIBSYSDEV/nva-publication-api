package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isChapterArticle;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.pages.Pages;

public class ChapterArticleBuilder extends AbstractPublicationInstanceBuilder {

    public ChapterArticleBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isChapterArticle(getCristinObject())) {
            return new ChapterArticle.Builder().build();
        } else {
            throw unknownSecondaryCategory();
        }
    }

    @Override
    protected CristinMainCategory getExpectedType() {
        return CristinMainCategory.CHAPTER;
    }
}
