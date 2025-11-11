package no.unit.nva.publication.create.pia;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;

public class ContributorUpdateService {

    private static final String SCOPUS_AUTHOR_ID = "scopus-auid";
    private final PiaClient piaClient;

    public ContributorUpdateService(PiaClient piaClient) {
        this.piaClient = piaClient;
    }

    public void updatePiaContributors(ImportCandidate input, ImportCandidate rawCandidate) {
        var rawContributors = rawCandidate.getEntityDescription().getContributors();
        var contributorsWithChanges = input.getEntityDescription()
            .getContributors()
            .stream()
            .filter(contributor -> hasCristinIdChange(contributor, rawContributors))
            .toList();
        
        if (!contributorsWithChanges.isEmpty()) {
            piaClient.updateContributor(contributorsWithChanges, rawCandidate.getScopusIdentifier().orElse(null));
        }
    }

    private boolean hasCristinIdChange(Contributor contributor, List<Contributor> rawContributors) {
        return rawContributors.stream()
            .anyMatch(raw -> hasSameAuidButDifferentCristinId(raw, contributor));
    }

    private boolean hasSameAuidButDifferentCristinId(Contributor a, Contributor b) {
        var cristinIdsDiffer = !Objects.equals(a.getIdentity().getId(), b.getIdentity().getId());
        var auidsMatch = Objects.equals(extractAuid(a), extractAuid(b));
        return cristinIdsDiffer && auidsMatch;
    }

    private Optional<AdditionalIdentifier> extractAuid(Contributor contributor) {
        return contributor.getIdentity()
            .getAdditionalIdentifiers()
            .stream()
            .filter(id -> SCOPUS_AUTHOR_ID.equals(id.sourceName()))
            .findFirst();
    }
}