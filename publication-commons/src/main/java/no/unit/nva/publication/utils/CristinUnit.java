package no.unit.nva.publication.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import no.unit.nva.commons.json.JsonSerializable;

/**
 * Represents a Cristin unit. JSON example: { "cristin_unit_id" : "5931.2.0.0", "unit_name" : { "en" : "Language Bank
 * and DH lab" }, "institution" : { "acronym" : "NB" }, "url" : "https://api.cristin.no/v2/units/5931.2.0.0", "acronym"
 * : "BS", "parent_unit" : { "cristin_unit_id" : "5931.0.0.0" } }
 */
@JsonInclude(Include.NON_NULL)
record CristinUnit(@JsonProperty(CRISTIN_UNIT_ID) String id,
                   ArrayList<CristinUnit> children,
                   @JsonProperty(PARENT_UNIT) CristinParentUnit parentUnit
)
    implements JsonSerializable {

    public static final String CRISTIN_UNIT_ID = "cristin_unit_id";
    public static final String PARENT_UNIT = "parent_unit";
}
