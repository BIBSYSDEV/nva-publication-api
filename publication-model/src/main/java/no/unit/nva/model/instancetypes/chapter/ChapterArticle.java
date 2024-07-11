package no.unit.nva.model.instancetypes.chapter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

import static java.util.Objects.isNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ChapterArticle implements PublicationInstance<Range> {

    public static final String PAGES_FIELD = "pages";
    private final Range pages;

    public ChapterArticle(Range pages) {
        this.pages = pages;
    }

    @JsonCreator
    public static ChapterArticle fromJson(@JsonProperty(PAGES_FIELD) Range pages,
                                          @JsonProperty("contentType") ChapterArticleContentType contentType) {
        if (ChapterArticleContentType.ACADEMIC_CHAPTER.equals(contentType)) {
            return new AcademicChapter(pages);
        } else if (ChapterArticleContentType.ENCYCLOPEDIA_CHAPTER.equals(contentType)) {
            return new EncyclopediaChapter(pages);
        } else if (ChapterArticleContentType.EXHIBITION_CATALOG_CHAPTER.equals(contentType)) {
            return new ExhibitionCatalogChapter(pages);
        } else if (ChapterArticleContentType.INTRODUCTION.equals(contentType)) {
            return new Introduction(pages);
        } else if (ChapterArticleContentType.NON_FICTION_CHAPTER.equals(contentType)) {
            return new NonFictionChapter(pages);
        } else if (ChapterArticleContentType.POPULAR_SCIENCE_CHAPTER.equals(contentType)) {
            return new PopularScienceChapter(pages);
        } else if (ChapterArticleContentType.TEXTBOOK_CHAPTER.equals(contentType)) {
            return new TextbookChapter(pages);
        } else if (isNull(contentType)) {
            return new AcademicChapter(pages);
        } else {
            throw new UnsupportedOperationException("The Chapter article subtype is unknown");
        }
    }

    @Override
    public Range getPages() {
        return pages;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChapterArticle)) {
            return false;
        }
        ChapterArticle that = (ChapterArticle) o;
        return Objects.equals(getPages(), that.getPages());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPages());
    }
}
