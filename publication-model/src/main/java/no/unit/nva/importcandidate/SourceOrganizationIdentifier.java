package no.unit.nva.importcandidate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
public record SourceOrganizationIdentifier(String affiliationIdentifier, String departmentIdentifier) {

}
