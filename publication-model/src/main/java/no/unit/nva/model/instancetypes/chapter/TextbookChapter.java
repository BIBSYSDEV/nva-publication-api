package no.unit.nva.model.instancetypes.chapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.model.pages.Range;

public class TextbookChapter extends ChapterArticle {

    public TextbookChapter(@JsonProperty(PAGES_FIELD) Range pages) {
        super(pages);
    }
}
