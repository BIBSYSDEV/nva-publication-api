package no.unit.nva.cristin.mapper;

import java.util.Set;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.instancetypes.chapter.AcademicChapter;
import no.unit.nva.model.instancetypes.chapter.EncyclopediaChapter;
import no.unit.nva.model.instancetypes.chapter.Introduction;
import no.unit.nva.model.instancetypes.chapter.NonFictionChapter;
import no.unit.nva.model.instancetypes.chapter.PopularScienceChapter;
import no.unit.nva.model.pages.Pages;
import no.unit.nva.model.pages.Range;

public class ChapterArticleBuilder extends AbstractPublicationInstanceBuilder {

    public ChapterArticleBuilder(CristinObject cristinObject) {
        super(cristinObject);
    }

    @Override
    public PublicationInstance<? extends Pages> build() {

        CristinSecondaryCategory secondaryCategory = getCristinObject().getSecondaryCategory();
        if (CristinSecondaryCategory.CHAPTER_ACADEMIC.equals(secondaryCategory)) {
            return new AcademicChapter(createChapterPages());
        } else if (CristinSecondaryCategory.CHAPTER.equals(secondaryCategory)) {
            return new NonFictionChapter(createChapterPages());
        } else if (CristinSecondaryCategory.POPULAR_CHAPTER_ARTICLE.equals(secondaryCategory)) {
            return new PopularScienceChapter(createChapterPages());
        } else if (CristinSecondaryCategory.LEXICAL_IMPORT.equals(secondaryCategory)) {
            return new EncyclopediaChapter(createChapterPages());
        } else if (CristinSecondaryCategory.FOREWORD.equals(secondaryCategory)
                   || CristinSecondaryCategory.INTRODUCTION.equals(secondaryCategory)) {
            return new Introduction(createChapterPages());
        } else {
            throw unknownSecondaryCategory();
        }
    }
    
    @Override
    protected Set<CristinMainCategory> getExpectedType() {
        return Set.of(CristinMainCategory.CHAPTER);
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
