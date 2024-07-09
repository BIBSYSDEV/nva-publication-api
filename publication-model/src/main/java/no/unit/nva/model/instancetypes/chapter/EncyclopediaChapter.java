package no.unit.nva.model.instancetypes.chapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.model.pages.Range;

public class EncyclopediaChapter extends ChapterArticle {
    public EncyclopediaChapter(@JsonProperty(PAGES_FIELD) Range pages) {
        super(pages);
    }
}
