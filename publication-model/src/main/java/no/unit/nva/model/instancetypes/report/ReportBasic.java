package no.unit.nva.model.instancetypes.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ReportBasic implements PublicationInstance<MonographPages> {

    private final MonographPages pages;

    public ReportBasic(@JsonProperty(PAGES_FIELD) MonographPages pages) {

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
        if (!(o instanceof ReportBasic)) {
            return false;
        }
        ReportBasic that = (ReportBasic) o;
        return Objects.equals(getPages(), that.getPages());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPages());
    }
}
