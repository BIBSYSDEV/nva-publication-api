package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConverter.extractContentString;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AffiliationType;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.OrganizationTp;
import no.sikt.nva.scopus.conversion.model.CorporationWithContributors;
import no.sikt.nva.scopus.conversion.model.cristin.SearchOrganizationResponse;
import no.sikt.nva.scopus.paralleliseutils.ParallelizeListProcessing;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AffiliationGenerator {

    public static final String NORWEGIAN_LAND_CODE = "NO";
    public static final String OTHER_INSTITUTIONS = "Andre institusjoner";
    public static final int SINGLE_HIT = 1;
    private static final Logger logger = LoggerFactory.getLogger(AffiliationGenerator.class);
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;

    public AffiliationGenerator(PiaConnection piaConnection,
                                CristinConnection cristinConnection) {
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
    }

    public List<CorporationWithContributors> getCorporations(List<AuthorGroupTp> authorGroupList) {
        var corporations = getCorporationsIds(authorGroupList);
        return fillCristinOrganizationData(corporations);
    }

    private static boolean isNotOtherNorwegianInstitution(CristinOrganization org) {
        return !(NORWEGIAN_LAND_CODE.equals(org.country()) && org.labels().containsValue(OTHER_INSTITUTIONS));
    }

    private static List<String> extractOrganizationNames(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup.getAffiliation())
                   .map(AffiliationType::getOrganization)
                   .map(AffiliationGenerator::extractNames)
                   .orElse(List.of());
    }

    private static String extractName(OrganizationTp org) {
        return extractContentString(org.getContent());
    }

    private static List<String> extractNames(List<OrganizationTp> organizationTps) {
        return organizationTps.stream().map(AffiliationGenerator::extractName).toList();
    }

    private static CristinOrganization getSingleOrganization(SearchOrganizationResponse searchResponse) {
        logger.info("Fetched cristin organization from author affiliation labels: {}", searchResponse);
        return searchResponse.hits().getFirst();
    }

    private static boolean hasSingleOrganization(SearchOrganizationResponse searchOrganizationResponse) {
        return searchOrganizationResponse.size() == SINGLE_HIT;
    }

    private List<CorporationWithContributors> getCorporationsIds(List<AuthorGroupTp> authorGroupList) {
        return ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(authorGroupList, this::retrieveCristinId);
    }

    private CorporationWithContributors retrieveCristinId(AuthorGroupTp authorGroupTp) {
        var corporation = new CorporationWithContributors();
        corporation.setScopusAuthors(authorGroupTp);
        corporation.setCristinOrganizationId(getCristinOrganizationUri(authorGroupTp).orElse(null));
        return corporation;
    }

    private List<CorporationWithContributors> fillCristinOrganizationData(
        List<CorporationWithContributors> corporations) {
        return ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(corporations,
                                                                              this::addCristinOrganisationData);
    }

    private CorporationWithContributors addCristinOrganisationData(CorporationWithContributors corporation) {
        return nonNull(corporation.getCristinOrganizationId())
                   ? fetCristinOrganizationsById(corporation)
                   : searchForCristinOrganizationByName(corporation);
    }

    private CorporationWithContributors searchForCristinOrganizationByName(CorporationWithContributors corporation) {
        var organizationNames = extractOrganizationNames(corporation.getScopusAuthors());
        var cristinOrganizations =
            organizationNames.stream()
                .map(this::searchCristinOrganization)
                .flatMap(Optional::stream)
                .toList();
        return corporation.copy().withCristinCorporations(cristinOrganizations).build();
    }

    private CorporationWithContributors fetCristinOrganizationsById(CorporationWithContributors corporation) {
        var organization = cristinConnection.fetchCristinOrganizationByCristinId(
            corporation.getCristinOrganizationId());
        List<CristinOrganization> validOrganizations = isValidOrganization(organization) ?
                                                           List.of(organization) : List.of();
        return corporation.copy().withCristinCorporations(validOrganizations).build();
    }

    private boolean isValidOrganization(CristinOrganization organization) {
        return nonNull(organization) && isNotOtherNorwegianInstitution(organization);
    }

    private Optional<CristinOrganization> searchCristinOrganization(String organization) {
        var searchResponse = cristinConnection.searchCristinOrganization(organization);
        return searchResponse.isPresent() && hasSingleOrganization(searchResponse.get())
                   ? Optional.ofNullable(getSingleOrganization(searchResponse.get()))
                   : Optional.empty();
    }

    private Optional<URI> getCristinOrganizationUri(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup)
                   .map(AuthorGroupTp::getAffiliation)
                   .map(AffiliationTp::getAfid)
                   .flatMap(piaConnection::fetchCristinOrganizationIdentifier);
    }
}
