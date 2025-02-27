package no.unit.nva.model.instancetypes.book;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.MonographPages;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class AcademicCommentary extends BookMonograph {

    public AcademicCommentary(@JsonProperty(PAGES_FIELD) MonographPages pages) {
        super(pages);
    }
}
