package no.sikt.nva.scopus.conversion.model;

import static java.util.Objects.isNull;
import static no.sikt.nva.scopus.ScopusConstants.AFFILIATION_DELIMITER;
import static no.sikt.nva.scopus.ScopusConverter.extractContentString;
import static no.sikt.nva.scopus.conversion.AffiliationMapper.mapToAffiliation;
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
import no.unit.nva.importcandidate.OrganizationMapping;
import no.unit.nva.model.Organization;
import no.unit.nva.model.UnconfirmedOrganization;

public class AuthorGroupWithCristinOrganization {

    private static final String NORWAY = "norway";

    private AuthorGroupTp scopusAuthors;

    private List<CristinOrganization> cristinOrganizations;
    private URI cristinOrganizationId;

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

    public List<OrganizationMapping> toCorporations() {
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

    private List<OrganizationMapping> generateCorporationFromCristinOrganization() {
        return cristinOrganizations.stream()
                   .map(CristinOrganization::id)
                   .map(Organization::fromUri)
                   .map(this::toImportorganization)
                   .distinct()
                   .collect(Collectors.toList());
    }

    private OrganizationMapping toImportorganization(Organization corporation) {
        return new OrganizationMapping(corporation, mapToAffiliation(scopusAuthors.getAffiliation()));
    }

    private List<OrganizationMapping> generateCorporationFromAuthorGroupTp() {
        var name = getOrganizationNameFromAuthorGroup();
        var labels = name.isPresent() && !name.get().isEmpty() ? name.map(
            organizationName -> Map.of(LanguageUtil.guessTheLanguageOfTheInputStringAsIso6391Code(organizationName),
                                       organizationName))
                         : extractCountryNameAsAffiliation();
        return isNotNorway(labels.orElse(Map.of())) && name.isPresent() && !name.get().isBlank()
                   ? List.of(new OrganizationMapping(new UnconfirmedOrganization(name.get()), mapToAffiliation(scopusAuthors.getAffiliation())))
                   : List.of();
    }

    private Optional<Map<String, String>> extractCountryNameAsAffiliation() {
        return Optional.ofNullable(scopusAuthors.getAffiliation())
                   .map(AffiliationTp::getCountry)
                   .map(country -> Map.of(LanguageUtil.guessTheLanguageOfTheInputStringAsIso6391Code(country),
                                          country));
    }

    private Optional<String> getOrganizationNameFromAuthorGroup() {
        return Optional.ofNullable(scopusAuthors.getAffiliation())
                   .map(AffiliationType::getOrganization)
                   .map(AuthorGroupWithCristinOrganization::toOrganizationName);
    }

    public static class Builder {

        private final AuthorGroupWithCristinOrganization corporation;

        public Builder() {
            this.corporation = new AuthorGroupWithCristinOrganization();
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

        public AuthorGroupWithCristinOrganization build() {
            return corporation;
        }
    }
}
