package no.unit.nva.model.instancetypes.degree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.pages.MonographPages;
import nva.commons.core.JacocoGenerated;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class OtherStudentWork extends DegreeBase {

    @JacocoGenerated
    @JsonCreator
    public OtherStudentWork(@JsonProperty(PAGES_FIELD) MonographPages pages,
                            @JsonProperty(SUBMITTED_DATE_FIELD) PublicationDate submittedDate) {
        super(pages, submittedDate);
    }
}
