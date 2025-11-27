package no.unit.nva.model.instancetypes.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ReportBookOfAbstract implements PublicationInstance<MonographPages> {

    private final MonographPages pages;

    /**
     * Book of abstracts: contains abstracts of presentations (lectures and posters) given at a specific conference.
     * Published by a publisher or by the conference itself.
     *
     * @param pages A description of the number of pages.
     */
    public ReportBookOfAbstract(@JsonProperty(PAGES_FIELD) MonographPages pages) {
        this.pages = pages;
    }

    @Override
    public MonographPages getPages() {
        return pages;
    }

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReportBookOfAbstract)) {
            return false;
        }
        ReportBookOfAbstract that = (ReportBookOfAbstract) o;
        return Objects.equals(getPages(), that.getPages());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPages());
    }
}
