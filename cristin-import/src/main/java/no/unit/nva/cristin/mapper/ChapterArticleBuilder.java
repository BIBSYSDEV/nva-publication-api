package no.unit.nva.cristin.mapper;

import static no.unit.nva.cristin.mapper.CristinSecondaryCategory.isChapterArticle;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.chapter.ChapterArticle;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;

public class ChapterArticleBuilder extends AbstractPublicationInstanceBuilder {

    public ChapterArticleBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {
        if (isChapterArticle(getCristinObject())) {
            return new ChapterArticle.Builder()
                       .withContentType(getCristinObject().getSecondaryCategory().toChapterArticleContentType())
                       .withPages(createChapterPages())
                       .withPeerReviewed(getCristinObject().isPeerReviewed())
                       .build();
        } else {
            throw unknownSecondaryCategory();
        }
    }
    
    @Override
    protected CristinMainCategory getExpectedType() {
        return CristinMainCategory.CHAPTER;
    }

    protected Range createChapterPages() {
        return new Range(extractPagesStart(), extractPagesEnd());
    }

    private String extractPagesStart() {
        return getCristinObject().getBookOrReportPartMetadata().getPagesStart();
    }

    private String extractPagesEnd() {
        return getCristinObject().getBookOrReportPartMetadata().getPagesEnd();
    }
}
