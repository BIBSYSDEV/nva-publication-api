package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.AFFILIATION_DELIMITER;
import static no.sikt.nva.scopus.ScopusConverter.extractContentString;
import static no.unit.nva.language.LanguageConstants.ENGLISH;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.AffiliationType;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.OrganizationTp;
import no.sikt.nva.scopus.conversion.model.cristin.CristinOrganization;
import no.sikt.nva.scopus.conversion.model.cristin.SearchOrganizationResponse;
import no.unit.nva.language.LanguageMapper;
import no.unit.nva.model.Organization;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AffiliationGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AffiliationGenerator.class);
    public static final int SINGLE_HIT = 1;

    private AffiliationGenerator() {
    }

    public static List<Organization> fromCristinOrganization(CristinOrganization cristinOrganization) {
        return List.of(new Organization.Builder().withId(cristinOrganization.getId())
                           .withLabels(cristinOrganization.getLabels())
                           .build());
    }

    public static List<Organization> fromAuthorGroupTp(AuthorGroupTp authorGroup, CristinConnection cristinConnection) {
        var organizationNames = extractOrganizationNames(authorGroup);
        var cristinOrganizations = fetchCristinOrganizations(cristinConnection, organizationNames);
        return cristinOrganizations.isEmpty()
                   ? getOrganizationsFromAuthorGroup(authorGroup)
                   : cristinOrganizations.stream().map(AffiliationGenerator::toOrganization).toList();
    }

    private static List<Organization> getOrganizationsFromAuthorGroup(AuthorGroupTp authorGroup) {
        return List.of(
            new Organization.Builder().withLabels(getOrganizationLabels(authorGroup).orElseGet(Map::of)).build());
    }

    private static Organization toOrganization(CristinOrganization cristinOrganization) {
        return new Organization.Builder()
                   .withId(cristinOrganization.getId())
                   .withLabels(cristinOrganization.getLabels())
                   .build();
    }

    private static List<CristinOrganization> fetchCristinOrganizations(CristinConnection cristinConnection,
                                                     List<String> organizationNames) {
        return organizationNames.stream()
                   .map(organization -> searchCristinOrganization(organization, cristinConnection))
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .toList();
    }

    private static Optional<CristinOrganization> searchCristinOrganization(String organization,
                                                                           CristinConnection cristinConnection) {
        var searchResponse = cristinConnection.searchCristinOrganization(organization);
        return searchResponse.isPresent() && hasSingleOrganization(searchResponse.get())
                   ? Optional.ofNullable(getSingleOrganization(searchResponse.get()))
                   : Optional.empty();
    }

    private static CristinOrganization getSingleOrganization(SearchOrganizationResponse searchResponse) {
        logger.info("Fetched cristin organization from author affiliation labels: {}", searchResponse);
        return searchResponse.hits().get(0);
    }

    private static boolean hasSingleOrganization(SearchOrganizationResponse searchOrganizationResponse) {
        return searchOrganizationResponse.size() == SINGLE_HIT;
    }

    @NotNull
    private static List<String> extractOrganizationNames(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup.getAffiliation())
                   .map(AffiliationType::getOrganization)
                   .map(AffiliationGenerator::extractNames)
                   .orElse(List.of());
    }

    private static List<String> extractNames(List<OrganizationTp> organizationTps) {
        return organizationTps.stream().map(AffiliationGenerator::extractName).toList();
    }

    private static String extractName(OrganizationTp org) {
        return extractContentString(org.getContent());
    }

    private static Optional<String> getOrganizationNameFromAuthorGroup(AuthorGroupTp authorGroup) {
        return Optional.ofNullable(authorGroup.getAffiliation())
                   .map(AffiliationType::getOrganization)
                   .map(AffiliationGenerator::toOrganizationName);
    }

    private static String toOrganizationName(List<OrganizationTp> organizationTps) {
        return organizationTps.stream()
                   .map(organizationTp -> extractContentString(organizationTp.getContent()))
                   .collect(Collectors.joining(AFFILIATION_DELIMITER));
    }

    private static Optional<Map<String, String>> getOrganizationLabels(AuthorGroupTp authorGroup) {
        return getOrganizationNameFromAuthorGroup(authorGroup).map(
            organizationName -> Map.of(getLanguageIso6391Code(organizationName), organizationName));
    }

    private static String getLanguageIso6391Code(String textToBeGuessedLanguageCodeFrom) {
        var detector = new OptimaizeLangDetector().loadModels();
        var result = detector.detect(textToBeGuessedLanguageCodeFrom);
        return result.isReasonablyCertain() ? getIso6391LanguageCodeForSupportedNvaLanguage(result.getLanguage())
                   : ENGLISH.getIso6391Code();
    }

    private static String getIso6391LanguageCodeForSupportedNvaLanguage(String possiblyUnsupportedLanguageIso6391code) {
        var language = LanguageMapper.getLanguageByIso6391Code(possiblyUnsupportedLanguageIso6391code);
        return nonNull(language.getIso6391Code()) ? language.getIso6391Code() : ENGLISH.getIso6391Code();
    }
}
