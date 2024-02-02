package no.unit.nva.expansion.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Map;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.Corporation;

@JsonTypeName(ExpandedImportCandidateOrganization.TYPE)
public class ExpandedImportCandidateOrganization extends Corporation {

    public static final String CONTENT_TYPE = "application/json";
    public static final String TYPE = "Organization";
    private final URI id;
    private final Map<String, String> labels;

    public ExpandedImportCandidateOrganization(URI id, Map<String, String> labels) {
        super();
        this.id = id;
        this.labels = labels;
    }

    public static ExpandedImportCandidateOrganization fromCristinOrganization(CristinOrganization cristinOrganization) {
        return new ExpandedImportCandidateOrganization(cristinOrganization.id(), cristinOrganization.labels());
    }
}
