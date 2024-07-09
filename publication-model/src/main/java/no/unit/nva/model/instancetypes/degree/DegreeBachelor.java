package no.unit.nva.model.instancetypes.degree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.pages.MonographPages;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class DegreeBachelor extends DegreeBase {

    public DegreeBachelor(@JsonProperty(PAGES_FIELD) MonographPages pages,
                          @JsonProperty(SUBMITTED_DATE_FIELD) PublicationDate submittedDate) {
        super(pages, submittedDate);
    }
}
