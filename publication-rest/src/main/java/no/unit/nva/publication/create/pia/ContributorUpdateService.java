package no.unit.nva.publication.create.pia;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import no.unit.nva.importcandidate.ImportCandidate;
import no.unit.nva.importcandidate.ImportContributor;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.publication.model.business.Resource;

public class ContributorUpdateService {

    private static final String SCOPUS_AUTHOR_ID = "scopus-auid";
    private final PiaClient piaClient;

    public ContributorUpdateService(PiaClient piaClient) {
        this.piaClient = piaClient;
    }

    public void updatePiaContributors(Resource resource, ImportCandidate rawCandidate) {
        var rawContributors = rawCandidate.getEntityDescription().contributors();
        var contributorsWithChanges = resource.getEntityDescription()
            .getContributors()
            .stream()
            .filter(contributor -> hasCristinIdChange(contributor, rawContributors))
            .toList();
        
        if (!contributorsWithChanges.isEmpty()) {
            piaClient.updateContributor(contributorsWithChanges, rawCandidate.getScopusIdentifier().orElse(null));
        }
    }

    private boolean hasCristinIdChange(Contributor contributor, Collection<ImportContributor> rawContributors) {
        return rawContributors.stream()
            .anyMatch(raw -> hasSameAuidButDifferentCristinId(raw, contributor));
    }

    private boolean hasSameAuidButDifferentCristinId(ImportContributor a, Contributor b) {
        var cristinIdsDiffer = !Objects.equals(a.identity().getId(), b.getIdentity().getId());
        var auidsMatch = Objects.equals(extractAuid(a.identity()), extractAuid(b.getIdentity()));
        return cristinIdsDiffer && auidsMatch;
    }

    private Optional<AdditionalIdentifier> extractAuid(Identity identity) {
        return identity
            .getAdditionalIdentifiers()
            .stream()
            .filter(id -> SCOPUS_AUTHOR_ID.equals(id.sourceName()))
            .findFirst();
    }
}