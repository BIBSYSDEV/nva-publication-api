package no.unit.nva.publication.create.pia;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportContributor;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;

public class ContributorUpdateService {

    private static final String SCOPUS_AUTHOR_ID = "scopus-auid";
    private final PiaClient piaClient;

    public ContributorUpdateService(PiaClient piaClient) {
        this.piaClient = piaClient;
    }

    public void updatePiaContributors(ImportCandidate input, ImportCandidate rawCandidate) {
        var rawContributors = rawCandidate.getEntityDescription().contributors();
        var contributorsWithChanges = input.getEntityDescription()
            .contributors()
            .stream()
            .filter(contributor -> hasCristinIdChange(contributor, rawContributors))
            .toList();
        
        if (!contributorsWithChanges.isEmpty()) {
            piaClient.updateContributor(contributorsWithChanges, rawCandidate.getScopusIdentifier().orElse(null));
        }
    }

    private boolean hasCristinIdChange(ImportContributor contributor, Collection<ImportContributor> rawContributors) {
        return rawContributors.stream()
            .anyMatch(raw -> hasSameAuidButDifferentCristinId(raw, contributor));
    }

    private boolean hasSameAuidButDifferentCristinId(ImportContributor a, ImportContributor b) {
        var cristinIdsDiffer = !Objects.equals(a.identity().getId(), b.identity().getId());
        var auidsMatch = Objects.equals(extractAuid(a), extractAuid(b));
        return cristinIdsDiffer && auidsMatch;
    }

    private Optional<AdditionalIdentifier> extractAuid(ImportContributor contributor) {
        return contributor.identity()
            .getAdditionalIdentifiers()
            .stream()
            .filter(id -> SCOPUS_AUTHOR_ID.equals(id.sourceName()))
            .findFirst();
    }
}