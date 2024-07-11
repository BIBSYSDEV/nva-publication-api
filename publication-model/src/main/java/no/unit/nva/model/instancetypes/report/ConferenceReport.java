package no.unit.nva.model.instancetypes.report;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.pages.MonographPages;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class ConferenceReport extends ReportBasic {
    public ConferenceReport(@JsonProperty(PAGES_FIELD) MonographPages pages) {
        super(pages);
    }
}
