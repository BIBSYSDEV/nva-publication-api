package no.unit.nva.model.instancetypes.journal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.Range;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class JournalIssue extends JournalNonPeerReviewedContent {
    /**
     * In order to represent entire issues of a journal — in cases where an institution has a special issue of a journal
     * featuring work from a group of researcher associated with the department, or a similar collection.
     * This class is used as a placeholder to hold information of such collections
     *
     * @param volume        Journal volume for the article.
     * @param issue         Journal issue for the article.
     * @param articleNumber Article number for the article.
     * @param pages         Page range for the article.
     */
    @JsonCreator
    public JournalIssue(@JsonProperty("volume") String volume,
                        @JsonProperty("issue") String issue,
                        @JsonProperty("articleNumber") String articleNumber,
                        @JsonProperty("pages") Range pages) {
        super(volume, issue, articleNumber, pages);
    }

    private JournalIssue(Builder builder) {
        this(builder.volume, builder.issue, builder.articleNumber, builder.pages);
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

        public JournalIssue build() {
            return new JournalIssue(this);
        }
    }
}
