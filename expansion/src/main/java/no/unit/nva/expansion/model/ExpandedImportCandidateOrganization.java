package no.unit.nva.expansion.model;

import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.Corporation;
import no.unit.nva.publication.external.services.UriRetriever;
import nva.commons.core.JacocoGenerated;

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

    public static ExpandedImportCandidateOrganization fromCristinId(URI cristinId) {
        return new ExpandedImportCandidateOrganization(cristinId, Map.of());
    }

    public static ExpandedImportCandidateOrganization fromCristinOrganization(CristinOrganization cristinOrganization) {
        return new ExpandedImportCandidateOrganization(cristinOrganization.id(), cristinOrganization.labels());
    }

    public ExpandedImportCandidateOrganization expand(UriRetriever uriRetriever) {
        return attempt(() -> uriRetriever.getRawContent(id, CONTENT_TYPE))
                   .map(Optional::orElseThrow)
                   .map(ExpandedImportCandidateOrganization::toCristinOrganization)
                   .map(ExpandedImportCandidateOrganization::fromCristinOrganization)
                   .orElse(failure -> this);
    }

    private static CristinOrganization toCristinOrganization(String string) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.readValue(string, CristinOrganization.class);
    }

    @JacocoGenerated
    public URI getId() {
        return id;
    }

    @JacocoGenerated
    public Map<String, String> getLabels() {
        return labels;
    }
}
