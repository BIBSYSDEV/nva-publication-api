package no.unit.nva.model.instancetypes.journal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

import static java.util.Objects.isNull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class JournalArticle implements PublicationInstance<Range> {
    public static final String PAGES_FIELD = "pages";
    public static final String TYPE = "type";
    public static final String VOLUME_FIELD = "volume";
    public static final String ISSUE_FIELD = "issue";
    public static final String ARTICLE_NUMBER_FIELD = "articleNumber";
    public static final String CONTENT_TYPE_FIELD = "contentType";
    private final Range pages;
    private final String volume;
    private final String issue;
    private final String articleNumber;

    protected JournalArticle(Range pages,
                             String volume,
                             String issue,
                             String articleNumber) {
        this.pages = pages;
        this.volume = volume;
        this.issue = issue;
        this.articleNumber = articleNumber;
    }

    @JsonCreator
    public static JournalArticle fromJson(@JsonProperty(PAGES_FIELD) Range pages,
                                          @JsonProperty(VOLUME_FIELD) String volume,
                                          @JsonProperty(ISSUE_FIELD) String issue,
                                          @JsonProperty(ARTICLE_NUMBER_FIELD) String articleNumber,
                                          @JsonProperty(CONTENT_TYPE_FIELD) JournalArticleContentType contentType) {
        if (JournalArticleContentType.ACADEMIC_ARTICLE.equals(contentType)) {
            return new AcademicArticle(pages, volume, issue, articleNumber);
        } else if (JournalArticleContentType.ACADEMIC_LITERATURE_REVIEW.equals(contentType)) {
            return new AcademicLiteratureReview(pages, volume, issue, articleNumber);
        } else if (JournalArticleContentType.CASE_REPORT.equals(contentType)) {
            return new CaseReport(pages, volume, issue, articleNumber);
        } else if (JournalArticleContentType.POPULAR_SCIENCE_ARTICLE.equals(contentType)) {
            return new PopularScienceArticle(pages, volume, issue, articleNumber);
        } else if (JournalArticleContentType.PROFESSIONAL_ARTICLE.equals(contentType)) {
            return new ProfessionalArticle(pages, volume, issue, articleNumber);
        } else if (JournalArticleContentType.STUDY_PROTOCOL.equals(contentType)) {
            return new StudyProtocol(pages, volume, issue, articleNumber);
        } else if (isNull(contentType)) {
            return new AcademicArticle(pages, volume, issue, articleNumber);
        } else {
            throw new UnsupportedOperationException("The Journal article subtype is unknown");
        }
    }

    public String getVolume() {
        return volume;
    }

    public String getIssue() {
        return issue;
    }

    public String getArticleNumber() {
        return articleNumber;
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
        if (!(o instanceof JournalArticle)) {
            return false;
        }
        JournalArticle that = (JournalArticle) o;
        return Objects.equals(getPages(), that.getPages())
                && Objects.equals(getVolume(), that.getVolume())
                && Objects.equals(getIssue(), that.getIssue())
                && Objects.equals(getArticleNumber(), that.getArticleNumber());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPages(), getVolume(), getIssue(), getArticleNumber());
    }
}
