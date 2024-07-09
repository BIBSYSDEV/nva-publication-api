package no.unit.nva.model.instancetypes.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.journal.JournalNonPeerReviewedContent;
import no.unit.nva.model.pages.Range;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class MediaReaderOpinion extends JournalNonPeerReviewedContent {


    /**
     * Creates an object that matches the Journal pattern, but throws an exception if peerReviewed is set to true.
     *
     * @param volume        Journal volume for the article.
     * @param issue         Journal issue for the article.
     * @param articleNumber Article number for the article.
     * @param pages         Page range for the article.
     */
    @JsonCreator
    public MediaReaderOpinion(@JsonProperty("volume") String volume,
                              @JsonProperty("issue") String issue,
                              @JsonProperty("articleNumber") String articleNumber,
                              @JsonProperty("pages") Range pages) {
        super(volume, issue, articleNumber, pages);
    }
}
