package no.unit.nva.model.instancetypes.journal;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import no.unit.nva.model.instancetypes.NonPeerReviewedPaper;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

public class JournalNonPeerReviewedContent extends NonPeerReviewedPaper implements JournalContent {

    @JsonProperty("volume")
    private String volume;
    @JsonProperty("issue")
    private String issue;
    @JsonProperty("articleNumber")
    private String articleNumber;

    /**
     * Creates an object that matches the Journal pattern, but throws an exception if peerReviewed is set to true.
     *
     * @param volume Journal volume for the article.
     * @param issue Journal issue for the article.
     * @param articleNumber Article number for the article.
     * @param pages Page range for the article.
     */
    public JournalNonPeerReviewedContent(
            String volume,
            String issue,
            String articleNumber,
            Range pages
    ) {
        super(pages);
        this.volume = volume;
        this.issue = issue;
        this.articleNumber = articleNumber;
    }

    @Override
    public void setVolume(String volume) {
        this.volume = volume;
    }

    @Override
    public String getVolume() {
        return volume;
    }

    @Override
    public void setIssue(String issue) {
        this.issue = issue;
    }

    @Override
    public String getIssue() {
        return issue;
    }

    @Override
    public void setArticleNumber(String articleNumber) {
        this.articleNumber = articleNumber;
    }

    @Override
    public String getArticleNumber() {
        return articleNumber;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JournalNonPeerReviewedContent)) {
            return false;
        }
        JournalNonPeerReviewedContent that = (JournalNonPeerReviewedContent) o;
        return Objects.equals(getVolume(), that.getVolume())
                && Objects.equals(getIssue(), that.getIssue())
                && Objects.equals(getArticleNumber(), that.getArticleNumber());
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(getVolume(), getIssue(), getArticleNumber());
    }
}
