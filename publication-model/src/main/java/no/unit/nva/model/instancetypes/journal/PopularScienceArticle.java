package no.unit.nva.model.instancetypes.journal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.Range;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PopularScienceArticle extends JournalArticle {

    public PopularScienceArticle(@JsonProperty(PAGES_FIELD) Range pages,
                                 @JsonProperty(VOLUME_FIELD) String volume,
                                 @JsonProperty(ISSUE_FIELD) String issue,
                                 @JsonProperty(ARTICLE_NUMBER_FIELD) String articleNumber) {
        super(pages, volume, issue, articleNumber);
    }
}
