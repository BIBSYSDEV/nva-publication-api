package no.unit.nva.model.instancetypes.degree;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.instancetypes.PublicationInstance;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

import java.util.Objects;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

public class DegreeBase implements PublicationInstance<MonographPages> {
    public static final String SUBMITTED_DATE_FIELD = "submittedDate";
    private final MonographPages pages;
    private final PublicationDate submittedDate;

    public DegreeBase(@JsonProperty(PAGES_FIELD) MonographPages pages,
                      @JsonProperty(SUBMITTED_DATE_FIELD) PublicationDate submittedDate) {
        this.pages = pages;
        this.submittedDate = submittedDate;
    }

    public PublicationDate getSubmittedDate() {
        return submittedDate;
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
        if (!(o instanceof DegreeBase)) {
            return false;
        }
        DegreeBase that = (DegreeBase) o;
        return Objects.equals(getPages(), that.getPages())
                && Objects.equals(getSubmittedDate(), that.getSubmittedDate());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getPages(), getSubmittedDate());
    }
}
