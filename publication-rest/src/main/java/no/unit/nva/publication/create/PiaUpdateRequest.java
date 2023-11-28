package no.unit.nva.publication.create;

import java.util.Optional;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.model.AdditionalIdentifier;
import no.unit.nva.model.Contributor;
import nva.commons.core.paths.UriWrapper;

public record PiaUpdateRequest(PiaPublication publication,
                               String cristinId,
                               String externalId,
                               String orcid,
                               int sequenceNr) implements JsonSerializable {

    public static PiaUpdateRequest toPiaRequest(Contributor contributor, String scopusId) {
        return new PiaUpdateRequest(createPiaPublication(scopusId),
                                    extractContributorCristinIdentifier(contributor),
                                    contributor.getIdentity().getOrcId(),
                                    extractScopusAuid(contributor),
                                    contributor.getSequence());
    }

    @Override
    public String toString() {
        return toJsonString();
    }

    private static PiaPublication createPiaPublication(String scopusId) {
        return new PiaPublication(scopusId, "SCOPUS");
    }

    private static String extractScopusAuid(Contributor contributor) {
        return extractAuid(contributor).get().getValue();
    }

    private static String extractContributorCristinIdentifier(Contributor contributor) {
        var cristinId = contributor.getIdentity().getId();
        return UriWrapper.fromUri(cristinId).getLastPathElement();
    }

    private static Optional<AdditionalIdentifier> extractAuid(Contributor contributor) {
        return contributor
                   .getIdentity()
                   .getAdditionalIdentifiers()
                   .stream()
                   .filter(additionalIdentifier ->
                               "scopus-auid".equalsIgnoreCase(additionalIdentifier.getSourceName()))
                   .findFirst();
    }
}

