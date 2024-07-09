package no.unit.nva.model.instancetypes.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.NullPages;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ConferenceLecture implements PublicationInstance<NullPages> {

    @JsonCreator
    public ConferenceLecture() {
        // Returns null pages so no point in setting pages.
    }

    @JsonGetter("pages")
    @Override
    public NullPages getPages() {
        return NullPages.NULL_PAGES;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return 222_222_222; // Implemented manually due to field-less class.
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ConferenceLecture;
    }
}
