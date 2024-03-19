package no.unit.nva.publication.model.utils;

import static java.util.Objects.nonNull;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Publication;
import no.unit.nva.publication.external.services.UriRetriever;
import no.unit.nva.publication.service.impl.CuratingInstitutionMigration;
import no.unit.nva.publication.utils.CristinUnitsUtil;
import no.unit.nva.publication.utils.RdfUtils;
import nva.commons.core.Environment;

public final class CuratingInstitutionsUtil {
    public static final String CRISTIN_BOT_FILTER_BYPASS_HEADER_NAME = "CRISTIN_BOT_FILTER_BYPASS_HEADER_NAME";
    public static final String CRISTIN_BOT_FILTER_BYPASS_HEADER_VALUE = "CRISTIN_BOT_FILTER_BYPASS_HEADER_VALUE";
    public static final URI CRISTIN_UNIT_API_URI = URI.create("https://api.cristin.no/v2/units");
    private final BiFunction<URI, UriRetriever, URI> topLevelFunction;
    private CuratingInstitutionsUtil() {
        this((uri, retriever) -> RdfUtils.getTopLevelOrgUri(retriever, uri));
    }

    private CuratingInstitutionsUtil(BiFunction<URI, UriRetriever, URI> topLevelSupplier) {
        this.topLevelFunction = topLevelSupplier;
    }

    public static CuratingInstitutionsUtil defaultCuratingInstitutionsUtil() {
        return new CuratingInstitutionsUtil((uri, retriever) -> RdfUtils.getTopLevelOrgUri(retriever, uri));
    }

    public static CuratingInstitutionsUtil defaultCuratingInstitutionsUtilWitchLargeCache(HttpClient httpClient, Environment environment) {
        return new CuratingInstitutionsUtil((uri, uriRetriever) -> getTopLevel(httpClient, environment, uri));
    }

    private static URI getTopLevel(HttpClient httpClient, Environment environment, URI uri) {
        return new CristinUnitsUtil(httpClient, CRISTIN_UNIT_API_URI, environment,
                                    CuratingInstitutionMigration.class,
                                    environment.readEnv(
                                        CRISTIN_BOT_FILTER_BYPASS_HEADER_NAME),
                                    environment.readEnv(
                                        CRISTIN_BOT_FILTER_BYPASS_HEADER_VALUE))
                   .getTopLevel(uri);
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
