package no.unit.nva.model.instancetypes.degree;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import no.unit.nva.model.PublicationDate;
import no.unit.nva.model.pages.MonographPages;

import static no.unit.nva.model.instancetypes.PublicationInstance.Constants.PAGES_FIELD;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class DegreeLicentiate extends DegreeBase {

    /**
     * Placeholder class for holding the details of resource type Licentiate thesis.
     * Licentiate thesis: A thesis for the licentiate's degree. It is given by some countries of the European Union,
     * Latin America and Syria. In Swedish and Finnish universities, a licentiate's degree is now recognised
     * as a pre-doctoral degree, in rank above the master's degree.
     *
     * @param pages A description of the number of pages.
     * @param submittedDate The date of submission for the thesis.
     */
    public DegreeLicentiate(@JsonProperty(PAGES_FIELD) MonographPages pages,
                            @JsonProperty(SUBMITTED_DATE_FIELD) PublicationDate submittedDate) {
        super(pages, submittedDate);
    }
}
