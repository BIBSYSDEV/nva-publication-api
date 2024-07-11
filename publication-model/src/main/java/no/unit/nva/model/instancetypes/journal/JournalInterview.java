package no.unit.nva.model.instancetypes.journal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.Range;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class JournalInterview extends JournalNonPeerReviewedContent {
    /**
     * Creates an object that matches the Journal pattern, but throws an exception if peerReviewed is set to true.
     *
     * @param volume        Journal volume for the article.
     * @param issue         Journal issue for the article.
     * @param articleNumber Article number for the article.
     * @param pages         Page range for the article.
     */
    public JournalInterview(@JsonProperty("volume") String volume,
                            @JsonProperty("issue") String issue,
                            @JsonProperty("articleNumber") String articleNumber,
                            @JsonProperty("pages") Range pages) {
        super(volume, issue, articleNumber, pages);
    }

    public static final class Builder {
        private Range pages;
        private String volume;
        private String issue;
        private String articleNumber;

        public Builder() {
        }

        public Builder withPages(Range pages) {
            this.pages = pages;
            return this;
        }

        public Builder withVolume(String volume) {
            this.volume = volume;
            return this;
        }

        public Builder withIssue(String issue) {
            this.issue = issue;
            return this;
        }

        public Builder withArticleNumber(String articleNumber) {
            this.articleNumber = articleNumber;
            return this;
        }

        public JournalInterview build() {
            return new JournalInterview(volume, issue, articleNumber, pages);
        }
    }
}
