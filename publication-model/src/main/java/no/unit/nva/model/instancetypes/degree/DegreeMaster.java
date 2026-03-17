package no.unit.nva.model.instancetypes.degree;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.pages.MonographPages;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class DegreeMaster extends DegreeBase {

  public DegreeMaster(
      @JsonProperty(PAGES_FIELD) MonographPages pages,
      @JsonProperty(SUBMITTED_DATE_FIELD) PublicationDate submittedDate) {
    super(pages, submittedDate);
  }
}
