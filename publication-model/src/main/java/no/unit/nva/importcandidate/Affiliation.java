package no.unit.nva.importcandidate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import no.unit.nva.model.Corporation;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record Affiliation(Corporation targetOrganization, SourceOrganization sourceOrganization) {

}
