package no.unit.nva.model.instancetypes.journal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class AcademicLiteratureReview extends JournalArticle {

    @JsonCreator
    public AcademicLiteratureReview(@JsonProperty(PAGES_FIELD) Range pages,
                                    @JsonProperty(VOLUME_FIELD) String volume,
                                    @JsonProperty(ISSUE_FIELD) String issue,
                                    @JsonProperty(ARTICLE_NUMBER_FIELD) String articleNumber) {
        super(pages, volume, issue, articleNumber);
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof AcademicLiteratureReview;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return 234_523_453;
    }
}
