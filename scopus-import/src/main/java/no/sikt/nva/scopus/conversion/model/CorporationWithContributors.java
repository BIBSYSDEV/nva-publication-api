package no.sikt.nva.scopus.conversion.model;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.AFFILIATION_DELIMITER;
import static no.sikt.nva.scopus.ScopusConverter.extractContentString;
import static no.unit.nva.language.LanguageConstants.ENGLISH;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AffiliationType;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.OrganizationTp;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.language.LanguageMapper;
import no.unit.nva.model.Corporation;
import no.unit.nva.model.Organization;
import no.unit.nva.model.UnconfirmedOrganization;
import org.apache.tika.langdetect.optimaize.OptimaizeLangDetector;

public class CorporationWithContributors {

    private static final String NORWAY = "norway";

    private AuthorGroupTp scopusAuthors;

    private List<CristinOrganization> cristinOrganizations;
    private URI cristinOrganizationId;

    public static String guessTheLanguageOfTheInputStringAsIso6391Code(String textToBeGuessedLanguageCodeFrom) {
        var detector = new OptimaizeLangDetector().loadModels();
        var result = detector.detect(textToBeGuessedLanguageCodeFrom);
        return result.isReasonablyCertain() ? getIso6391LanguageCodeForSupportedNvaLanguage(result.getLanguage())
                   : ENGLISH.getIso6391Code();
    }

    public AuthorGroupTp getScopusAuthors() {
        return scopusAuthors;
    }

    public void setScopusAuthors(AuthorGroupTp scopusAuthors) {
        this.scopusAuthors = scopusAuthors;
    }

    public List<CristinOrganization> getCristinOrganizations() {
        return isNull(cristinOrganizations)
                   ? List.of()
                   : cristinOrganizations;
    }

    public void setCristinOrganizations(List<CristinOrganization> cristinOrganizations) {
        this.cristinOrganizations = cristinOrganizations;
    }

    public URI getCristinOrganizationId() {
        return cristinOrganizationId;
    }

    public void setCristinOrganizationId(URI cristinId) {
        this.cristinOrganizationId = cristinId;
    }

    public List<Corporation> toCorporations() {
        return isNull(cristinOrganizations) || cristinOrganizations.isEmpty()
                   ? generateCorporationFromAuthorGroupTp()
                   : generateCorporationFromCristinOrganization();
    }

    public Builder copy() {
        return new Builder().withAuthorGroupTp(scopusAuthors)
                   .withCristinOrganizationId(cristinOrganizationId)
                   .withCristinCorporations(cristinOrganizations);
    }

    private static boolean isNotNorway(Map<String, String> labels) {
        return labels.values().stream().noneMatch(NORWAY::equalsIgnoreCase);
    }

    private static String toOrganizationName(List<OrganizationTp> organizationTps) {
        return organizationTps.stream()
                   .map(organizationTp -> extractContentString(organizationTp.getContent()))
                   .collect(Collectors.joining(AFFILIATION_DELIMITER));
    }

    private static String getIso6391LanguageCodeForSupportedNvaLanguage(String possiblyUnsupportedLanguageIso6391code) {
        var language = LanguageMapper.getLanguageByIso6391Code(possiblyUnsupportedLanguageIso6391code);
        return nonNull(language.getIso6391Code()) ? language.getIso6391Code() : ENGLISH.getIso6391Code();
    }

    private List<Corporation> generateCorporationFromCristinOrganization() {
        return cristinOrganizations.stream()
                   .map(cristinOrganization -> new Organization.Builder().withId(cristinOrganization.id()).build())
                   .collect(Collectors.toList());
    }

    private List<Corporation> generateCorporationFromAuthorGroupTp() {
        var name = getOrganizationNameFromAuthorGroup();
        var labels = name.isPresent() && !name.get().isEmpty() ? name.map(
            organizationName -> Map.of(guessTheLanguageOfTheInputStringAsIso6391Code(organizationName),
                                       organizationName))
                         : extractCountryNameAsAffiliation();
        return isNotNorway(labels.orElse(Map.of()))
                   ? List.of(new UnconfirmedOrganization(name.orElse(null)))
                   : List.of();
    }

    private Optional<Map<String, String>> extractCountryNameAsAffiliation() {
        return Optional.ofNullable(scopusAuthors.getAffiliation())
                   .map(AffiliationTp::getCountry)
                   .map(country -> Map.of(guessTheLanguageOfTheInputStringAsIso6391Code(country), country));
    }

    private Optional<String> getOrganizationNameFromAuthorGroup() {
        return Optional.ofNullable(scopusAuthors.getAffiliation())
                   .map(AffiliationType::getOrganization)
                   .map(CorporationWithContributors::toOrganizationName);
    }

    public static class Builder {

        private final CorporationWithContributors corporation;

        public Builder() {
            this.corporation = new CorporationWithContributors();
        }

        public Builder withAuthorGroupTp(AuthorGroupTp authorGroupTp) {
            corporation.setScopusAuthors(authorGroupTp);
            return this;
        }

        public Builder withCristinOrganizationId(URI cristinOrganizationId) {
            corporation.setCristinOrganizationId(cristinOrganizationId);
            return this;
        }

        public Builder withCristinCorporations(List<CristinOrganization> cristinOrganizations) {
            corporation.setCristinOrganizations(cristinOrganizations);
            return this;
        }

        public CorporationWithContributors build() {
            return corporation;
        }
    }
}
