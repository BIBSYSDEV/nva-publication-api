package no.unit.nva.publication.model.utils;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;

public final class CuratingInstitutionsUtil {
    private final BiFunction<URI, UriRetriever, URI> topLevelFunction;
    public CuratingInstitutionsUtil(BiFunction<URI, UriRetriever, URI> topLevelSupplier) {
        this.topLevelFunction = topLevelSupplier;
    }

    public Set<URI> getCuratingInstitutions(Publication publication, UriRetriever uriRetriever) {
        return getVerifiedContributors(publication)
                   .flatMap(CuratingInstitutionsUtil::getOrganizationIds)
                   .collect(Collectors.toSet())
                   .parallelStream()
                   .map(orgId -> topLevelFunction.apply(orgId, uriRetriever))
                   .collect(Collectors.toSet());
    }

    private static Stream<URI> getOrganizationIds(Contributor contributor) {
        return contributor.getAffiliations().stream()
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId);
    }

    private static Stream<Contributor> getVerifiedContributors(Publication publication) {
        return publication.getEntityDescription().getContributors()
                   .stream()
                   .filter(CuratingInstitutionsUtil::isVerifiedContributor);
    }

    private static boolean isVerifiedContributor(Contributor contributor) {
        return nonNull(contributor.getIdentity()) && nonNull(contributor.getIdentity().getId());
    }
}
