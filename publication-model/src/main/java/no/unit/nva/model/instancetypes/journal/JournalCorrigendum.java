package no.unit.nva.model.instancetypes.journal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.net.URI;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

/**
 * A Journal corrigendum is a correction to a previously published Journal article/content.
 *
 * <p>They have their own DOIs and are viewed as independent publications.
 *
 * <p>Example: http://doi.org/10.1038/nature10098
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class JournalCorrigendum extends JournalNonPeerReviewedContent {

    private final URI corrigendumFor;

    /**
     * Creates an object that matches the Journal pattern, but throws an exception if peerReviewed is set to true.
     *
     * @param volume        Journal volume for the article.
     * @param issue         Journal issue for the article.
     * @param articleNumber Article number for the article.
     * @param pages         Page range for the article.
     * @param corrigendumFor    The linked resource that the corrigendum amends.
     */
    public JournalCorrigendum(@JsonProperty("volume") String volume,
                              @JsonProperty("issue") String issue,
                              @JsonProperty("articleNumber") String articleNumber,
                              @JsonProperty("pages") Range pages,
                              @JsonProperty("corrigendumFor") URI corrigendumFor) {
        super(volume, issue, articleNumber, pages);
        this.corrigendumFor = corrigendumFor;
    }

    private JournalCorrigendum(Builder builder) {
        this(builder.volume, builder.issue, builder.articleNumber, builder.pages, builder.corrigendumFor);
    }

    @JacocoGenerated
    public URI getCorrigendumFor() {
        return corrigendumFor;
    }

    public static final class Builder {
        private Range pages;
        private String volume;
        private String issue;
        private String articleNumber;
        private URI corrigendumFor;

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

        public Builder withCorrigendumFor(URI corrigendumFor) {
            this.corrigendumFor = corrigendumFor;
            return this;
        }

        public JournalCorrigendum build() {
            return new JournalCorrigendum(this);
        }
    }
}
