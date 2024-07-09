package no.unit.nva.model.instancetypes.chapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.Range;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class NonFictionChapter extends ChapterArticle {
    public NonFictionChapter(@JsonProperty(PAGES_FIELD) Range pages) {
        super(pages);
    }
}
