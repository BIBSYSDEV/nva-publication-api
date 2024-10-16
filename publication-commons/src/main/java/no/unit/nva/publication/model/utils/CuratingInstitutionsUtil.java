package no.unit.nva.publication.model.utils;

import static java.util.Objects.nonNull;
import static no.unit.nva.publication.utils.RdfUtils.getTopLevelOrgUri;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.CuratingInstitution;
import no.unit.nva.model.EntityDescription;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.utils.CristinUnitsUtil;

public final class CuratingInstitutionsUtil {
    private CuratingInstitutionsUtil() {
    }

    public static Set<CuratingInstitution> getCuratingInstitutionsOnline(Publication publication, UriRetriever uriRetriever) {
        return getVerifiedContributors(publication.getEntityDescription())

                   .flatMap(contributor -> toCuratingInstitutionOnline(contributor, uriRetriever))
                   .collect(Collectors.groupingBy(SimpleEntry::getKey,
                                                  Collectors.mapping(SimpleEntry::getValue, Collectors.toList())))
                   .entrySet()
                   .stream()
                   .map(entry -> new CuratingInstitution(entry.getKey(), entry.getValue()))
                   .collect(Collectors.toSet());
    }

    public static Set<CuratingInstitution> getCuratingInstitutionsCached(EntityDescription entityDescription,
                                                                         CristinUnitsUtil cristinUnitsUtil) {
        return getVerifiedContributors(entityDescription)
                   .flatMap(contributor -> toCuratingInstitution(contributor, cristinUnitsUtil))
                   .collect(Collectors.groupingBy(SimpleEntry::getKey,
                                                  Collectors.mapping(SimpleEntry::getValue, Collectors.toList())))
                   .entrySet()
                   .stream()
                   .map(entry -> new CuratingInstitution(entry.getKey(), entry.getValue()))
                   .collect(Collectors.toSet());
    }

    private static Stream<SimpleEntry<URI, URI>> toCuratingInstitution(Contributor contributor,
                                                                       CristinUnitsUtil cristinUnitsUtil) {
        return getOrganizationIds(contributor)
                              .map(cristinUnitsUtil::getTopLevel)
                              .filter(Objects::nonNull)
                              .map(id -> new SimpleEntry<>(id, contributor.getIdentity().getId()));
    }

    private static Stream<SimpleEntry<URI, URI>> toCuratingInstitutionOnline(Contributor contributor,
                                                                             UriRetriever uriRetriever) {
        return getOrganizationIds(contributor)
                   .map(orgId -> getTopLevelOrgUri(uriRetriever, orgId))
                   .filter(Objects::nonNull)
                   .map(id -> new SimpleEntry<>(id, contributor.getIdentity().getId()));
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
