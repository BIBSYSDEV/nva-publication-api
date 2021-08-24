package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinMainCategory.isChapter;
import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isChapterArticle;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.pages.Pages;

public class ChapterArticleBuilder implements PublicationInstanceBuilder {

    private final CristinObject cristinObject;

    public ChapterArticleBuilder(CristinObject cristinObject) {
        if (!isChapter(cristinObject)) {
            throw new UnsupportedOperationException("Not a chapter");
        }
        this.cristinObject = cristinObject;
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isChapterArticle(cristinObject)) {
            return new ChapterArticle.Builder().build();
        } else {
            throw unknownSecondaryCategory();
        }
    }
}
