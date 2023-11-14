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
import no.unit.nva.language.LanguageMapper;
import no.unit.nva.model.Organization;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;

public final class AffiliationGenerator {

    private AffiliationGenerator() {
    }

    public static List<Organization> fromCristinOrganization(CristinOrganization cristinOrganization) {
        return List.of(new Organization.Builder().withId(cristinOrganization.getId())
                           .withLabels(cristinOrganization.getLabels())
                           .build());
    }

    public static List<Organization> fromAuthorGroupTp(AuthorGroupTp authorGroup) {
        return List.of(
            new Organization.Builder().withLabels(getOrganizationLabels(authorGroup).orElseGet(Map::of)).build());
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
