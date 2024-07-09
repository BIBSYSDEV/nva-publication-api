package no.unit.nva.model.instancetypes.media;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.NullPages;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class MediaBase implements PublicationInstance<NullPages> {

    public static final String PAGES_FIELD = "pages";

    @JsonCreator
    public MediaBase() {
        // Since this class returns a null object for pages, the value is not set.
    }

    @JsonGetter(PAGES_FIELD)
    @Override
    public NullPages getPages() {
        return NullPages.NULL_PAGES;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return obj instanceof MediaBase;
    }
}
