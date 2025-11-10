package no.unit.nva.publication.create.pia;

import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.publication.model.business.importcandidate.ImportContributor;
import nva.commons.core.paths.UriWrapper;

public record PiaUpdateRequest(PiaPublication publication,
                               String cristinId,
                               String externalId,
                               String orcid,
                               int sequenceNr) implements JsonSerializable {

    public static PiaUpdateRequest toPiaRequest(ImportContributor contributor, String scopusId) {
        return new PiaUpdateRequest(createPiaPublication(scopusId),
                                    extractContributorCristinIdentifier(contributor),
                                    contributor.identity().getOrcId(),
                                    extractScopusAuid(contributor),
                                    contributor.sequence());
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    private static PiaPublication createPiaPublication(String scopusId) {
        return new PiaPublication(scopusId, "SCOPUS");
    }

    private static String extractScopusAuid(ImportContributor contributor) {
        return extractAuid(contributor).orElseThrow().value();
    }

    private static String extractContributorCristinIdentifier(ImportContributor contributor) {
        var cristinId = contributor.identity().getId();
        return UriWrapper.fromUri(cristinId).getLastPathElement();
    }

    private static Optional<AdditionalIdentifier> extractAuid(ImportContributor contributor) {
        return contributor
                   .identity()
                   .getAdditionalIdentifiers()
                   .stream()
                   .filter(additionalIdentifier ->
                               "scopus-auid".equalsIgnoreCase(additionalIdentifier.sourceName()))
                   .findFirst();
    }
}

