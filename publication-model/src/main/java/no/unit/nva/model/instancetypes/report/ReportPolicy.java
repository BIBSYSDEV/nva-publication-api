package no.unit.nva.model.instancetypes.report;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.MonographPages;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ReportPolicy extends ReportBasic {

  public ReportPolicy(@JsonProperty(PAGES_FIELD) MonographPages pages) {
    super(pages);
  }
}
