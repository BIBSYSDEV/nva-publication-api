package no.unit.nva.publication.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
record CristinParentUnit(@JsonProperty("cristin_unit_id") String id) {

}
