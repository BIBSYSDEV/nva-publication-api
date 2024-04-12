package no.unit.nva.publication.model.utils;

import static no.unit.nva.publication.utils.RdfUtils.getTopLevelOrgUri;
import java.net.URI;
import static java.util.Objects.nonNull;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.utils.CristinUnitsUtil;

public final class CuratingInstitutionsUtil {
    private CuratingInstitutionsUtil() {
    }

    public static Set<URI> getCuratingInstitutionsOnline(Publication publication, UriRetriever uriRetriever) {
        return getVerifiedContributors(publication.getEntityDescription())
                   .flatMap(CuratingInstitutionsUtil::getOrganizationIds)
                   .collect(Collectors.toSet())
                   .parallelStream()
                   .map(orgId -> getTopLevelOrgUri(uriRetriever, orgId))
                   .filter(Objects::nonNull)
                   .collect(Collectors.toSet());
    }

    public static Set<URI> getCuratingInstitutionsCached(EntityDescription entityDescription,
                                                         CristinUnitsUtil cristinUnitsUtil) {
        return getVerifiedContributors(entityDescription)
                   .flatMap(CuratingInstitutionsUtil::getOrganizationIds)
                   .map(cristinUnitsUtil::getTopLevel)
                   .filter(Objects::nonNull)
                   .collect(Collectors.toSet());
    }

    private static Stream<URI> getOrganizationIds(Contributor contributor) {
        return contributor.getAffiliations().stream()
                   .filter(Organization.class::isInstance)
                   .map(Organization.class::cast)
                   .map(Organization::getId);
    }

    private static Stream<Contributor> getVerifiedContributors(EntityDescription entityDescription) {
        return Optional.ofNullable(entityDescription)
                   .map(EntityDescription::getContributors)
                   .orElse(Collections.emptyList())
                   .stream()
                   .filter(CuratingInstitutionsUtil::isVerifiedContributor);
    }

    private static boolean isVerifiedContributor(Contributor contributor) {
        return nonNull(contributor.getIdentity()) && nonNull(contributor.getIdentity().getId());
    }
}
