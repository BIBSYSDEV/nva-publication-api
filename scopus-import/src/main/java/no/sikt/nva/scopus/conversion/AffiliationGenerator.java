package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConverter.extractContentString;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AffiliationType;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.OrganizationTp;
import no.sikt.nva.scopus.conversion.model.AuthorGroupWithCristinOrganization;
import no.unit.nva.publication.external.services.cristin.SearchOrganizationResponse;
import no.sikt.nva.scopus.paralleliseutils.ParallelizeListProcessing;
import no.unit.nva.publication.external.services.cristin.CristinConnection;
import no.unit.nva.publication.external.services.cristin.CristinOrganization;

public class AffiliationGenerator {

    public static final String NORWEGIAN_LAND_CODE = "NO";
    public static final String OTHER_INSTITUTIONS = "Andre institusjoner";
    private final PiaConnection piaConnection;
    private final CristinConnection cristinConnection;

    public AffiliationGenerator(PiaConnection piaConnection, CristinConnection cristinConnection) {
        this.piaConnection = piaConnection;
        this.cristinConnection = cristinConnection;
    }

    public List<AuthorGroupWithCristinOrganization> getCorporations(List<AuthorGroupTp> authorGroupList) {
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

    private Function<List<CristinOrganization>, Optional<CristinOrganization>> collectSingleEntryOrOptionalEmpty() {
        return list -> list.size() == 1 ? Optional.of(list.getFirst()) : Optional.empty();
    }

    private List<AuthorGroupWithCristinOrganization> getCorporationsIds(List<AuthorGroupTp> authorGroupList) {
        return ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(authorGroupList, this::retrieveCristinId);
    }

    private AuthorGroupWithCristinOrganization retrieveCristinId(AuthorGroupTp authorGroupTp) {
        var corporation = new AuthorGroupWithCristinOrganization();
        corporation.setScopusAuthors(authorGroupTp);
        corporation.setCristinOrganizationId(getCristinOrganizationUri(authorGroupTp).orElse(null));
        return corporation;
    }

    private List<AuthorGroupWithCristinOrganization> fillCristinOrganizationData(
        List<AuthorGroupWithCristinOrganization> corporations) {
        return ParallelizeListProcessing.runAsVirtualNetworkingCallingThreads(corporations,
                                                                              this::addCristinOrganisationData);
    }

    private AuthorGroupWithCristinOrganization addCristinOrganisationData(AuthorGroupWithCristinOrganization corporation) {
        var cristinOrganization = fetchCristinOrganizationsById(corporation)
            .or(() -> searchForCristinOrganizationByName(corporation))
            .or(() -> searchForCountry(corporation));

        return cristinOrganization.isPresent()
                   ? corporation.copy().withCristinCorporations(List.of(cristinOrganization.get())).build()
                   : corporation;
    }

    //Countries are stored as organizations in Cristin, we use the same endpoint
    //for fetching countries as we do with organizations
    private Optional<CristinOrganization> searchForCountry(
        AuthorGroupWithCristinOrganization corporation) {
        return Optional.ofNullable(corporation.getScopusAuthors())
                   .map(AuthorGroupTp::getAffiliation)
                   .map(AffiliationTp::getCountry)
                   .flatMap(this::searchCristinOrganization);
    }

    private Optional<CristinOrganization> searchForCristinOrganizationByName(
        AuthorGroupWithCristinOrganization corporation) {
        return extractOrganizationNames(corporation.getScopusAuthors()).stream()
                   .map(this::searchCristinOrganization)
                   .flatMap(Optional::stream)
                   .findFirst();
    }

    private Optional<CristinOrganization> fetchCristinOrganizationsById(AuthorGroupWithCristinOrganization corporation) {
        return Optional.ofNullable(cristinConnection.fetchCristinOrganizationByCristinId(
            corporation.getCristinOrganizationId()))
                   .filter(this::isValidOrganization);
    }

    private boolean isValidOrganization(CristinOrganization organization) {
        return nonNull(organization) && isNotOtherNorwegianInstitution(organization);
    }

    private Optional<CristinOrganization> searchCristinOrganization(String organization) {
        return cristinConnection.searchCristinOrganization(organization)
                   .map(SearchOrganizationResponse::hits)
                   .orElse(Collections.emptyList())
                   .stream()
                   .filter(cristinOrg -> cristinOrg.containsLabelWithValue(organization))
                   .collect(Collectors.collectingAndThen(Collectors.toList(), collectSingleEntryOrOptionalEmpty()));
    }

    private Optional<URI> getCristinOrganizationUri(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup)
                   .map(AuthorGroupTp::getAffiliation)
                   .map(AffiliationTp::getAfid)
                   .flatMap(piaConnection::fetchCristinOrganizationIdentifier);
    }
}
