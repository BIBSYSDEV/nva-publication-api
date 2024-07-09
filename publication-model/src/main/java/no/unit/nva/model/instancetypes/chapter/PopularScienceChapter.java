package no.unit.nva.model.instancetypes.chapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.Range;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class PopularScienceChapter extends ChapterArticle {

    public PopularScienceChapter(@JsonProperty(PAGES_FIELD) Range pages) {
        super(pages);
    }
}
