package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Map;
import no.unit.nva.expansion.model.cristin.CristinOrganization;


@JsonTypeName(ExpandedImportCandidateOrganization.TYPE)
@JsonTypeInfo(use = Id.NAME, property = "type")
public record ExpandedImportCandidateOrganization(URI id, Map<String, String> labels) {

    public static final String TYPE = "Organization";

    public static ExpandedImportCandidateOrganization fromCristinOrganization(CristinOrganization cristinOrganization) {
        return new ExpandedImportCandidateOrganization(cristinOrganization.id(), cristinOrganization.labels());
    }


}
