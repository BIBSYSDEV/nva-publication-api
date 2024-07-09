package no.unit.nva.model.instancetypes.chapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.Range;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

@JsonTypeInfo(use = Id.NAME, property = "type")
public class ChapterConferenceAbstract implements PublicationInstance<Range> {

    private final Range pages;

    /**
     * Conference abstract as Chapter: an abstract of a presentation given at a conference and published as a Chapter.
     *
     * @param pages A description of the number of pages.
     */
    public ChapterConferenceAbstract(@JsonProperty(PAGES_FIELD) Range pages) {

        this.pages = pages;
    }

    @Override
    public Range getPages() {
        return pages;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ChapterConferenceAbstract)) {
            return false;
        }
        ChapterConferenceAbstract that = (ChapterConferenceAbstract) o;
        return Objects.equals(getPages(), that.getPages());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPages());
    }
}
