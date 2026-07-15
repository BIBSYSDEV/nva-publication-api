package no.unit.nva.cristin.mapper;

import java.util.Optional;
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
    if (CristinSecondaryCategory.CHAPTER_ACADEMIC == secondaryCategory) {
      return new AcademicChapter(createChapterPages());
    } else if (CristinSecondaryCategory.CHAPTER == secondaryCategory) {
      return new NonFictionChapter(createChapterPages());
    } else if (CristinSecondaryCategory.POPULAR_CHAPTER_ARTICLE == secondaryCategory) {
      return new PopularScienceChapter(createChapterPages());
    } else if (CristinSecondaryCategory.LEXICAL_IMPORT == secondaryCategory) {
      return new EncyclopediaChapter(createChapterPages());
    } else if (CristinSecondaryCategory.FOREWORD == secondaryCategory
        || CristinSecondaryCategory.INTRODUCTION == secondaryCategory) {
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
    return Optional.ofNullable(getCristinObject())
        .map(CristinObject::getBookOrReportPartMetadata)
        .map(CristinBookOrReportPartMetadata::getPagesStart)
        .orElse(null);
  }

  private String extractPagesEnd() {
    return Optional.ofNullable(getCristinObject())
        .map(CristinObject::getBookOrReportPartMetadata)
        .map(CristinBookOrReportPartMetadata::getPagesEnd)
        .orElse(null);
  }
}
