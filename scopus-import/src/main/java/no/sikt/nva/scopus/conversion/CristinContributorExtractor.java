package no.sikt.nva.scopus.conversion;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static no.sikt.nva.scopus.ScopusConstants.ORCID_DOMAIN_URL;
import static nva.commons.core.StringUtils.isNotBlank;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.AuthorGroupTp;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.conversion.model.AuthorGroupWithCristinOrganization;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.importcandidate.ImportContributor;
import no.unit.nva.importcandidate.ImportOrganization;
import no.unit.nva.importcandidate.ScopusAffiliation;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.additionalidentifiers.AdditionalIdentifier;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public final class CristinContributorExtractor {

    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final String ORCID_FIELD_NAME = "orcid";
    public static final String SCOPUS_AUID = "scopus-auid";

    @JacocoGenerated
    private CristinContributorExtractor() {
    }

    public static ImportContributor generateContributorFromCristinPerson(
        CristinPerson cristinPerson, AuthorTp authorTp, PersonalnameType correspondencePerson,
        AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization) {
        return new ImportContributor(generateContributorIdentityFromCristinPerson(cristinPerson, authorTp),
                                     generateOrganizations(cristinPerson.getAffiliations(), authorGroupWithCristinOrganization),
                                     new RoleType(Role.CREATOR),
                                     getSequenceNumber(authorTp),
                                     isCorrespondingAuthor(authorTp, correspondencePerson));
    }

    private static Identity generateContributorIdentityFromCristinPerson(CristinPerson cristinPerson,
                                                                         AuthorTp authorTp) {
        var identity = new Identity();
        identity.setName(determineContributorName(cristinPerson));
        identity.setOrcId(extractOrcId(cristinPerson, authorTp));
        identity.setId(cristinPerson.getId());
        identity.setAdditionalIdentifiers(extractAdditionalIdentifiers(authorTp));
        identity.setVerificationStatus(nonNull(cristinPerson.getVerified()) ? generateVerificationStatus(cristinPerson)
                                           : ContributorVerificationStatus.CANNOT_BE_ESTABLISHED);
        return identity;
    }

    private static List<AdditionalIdentifier> extractAdditionalIdentifiers(AuthorTp authorTp) {
        return isNotBlank(authorTp.getAuid()) ? List.of(createAuidAdditionalIdentifier(authorTp)) : emptyList();
    }

    private static AdditionalIdentifier createAuidAdditionalIdentifier(AuthorTp authorTp) {
        return new AdditionalIdentifier(SCOPUS_AUID, authorTp.getAuid());
    }

    private static String extractOrcId(CristinPerson cristinPerson, AuthorTp authorTp) {
        return extractOrcIdFromCristinPerson(cristinPerson)
                   .or(() -> extractOrcIdFromAuthorTp(authorTp))
                   .orElse(null);
    }

    private static Optional<String> extractOrcIdFromAuthorTp(AuthorTp authorTp) {
        return Optional.ofNullable(authorTp)
                   .map(AuthorTp::getOrcid)
                   .map(CristinContributorExtractor::craftOrcidUriString);
    }

    private static String craftOrcidUriString(String potentiallyMalformedOrcidString) {
        return potentiallyMalformedOrcidString.contains(ORCID_DOMAIN_URL)
                   ? potentiallyMalformedOrcidString
                   : ORCID_DOMAIN_URL + potentiallyMalformedOrcidString;
    }

    private static Optional<String> extractOrcIdFromCristinPerson(CristinPerson cristinPerson) {
        return Optional.ofNullable(cristinPerson.getIdentifiers()).stream()
                   .flatMap(Collection::stream)
                   .filter(CristinContributorExtractor::isOrcid)
                   .findAny()
                   .map(TypedValue::getValue);
    }

    private static ContributorVerificationStatus generateVerificationStatus(CristinPerson cristinPerson) {
        return Boolean.TRUE.equals(cristinPerson.getVerified())
                   ? ContributorVerificationStatus.VERIFIED
                   : ContributorVerificationStatus.NOT_VERIFIED;
    }

    private static List<ImportOrganization> generateOrganizations(Set<Affiliation> affiliations,
                                                                  AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization) {
        var cristinPersonActiveAffiliations = createOrganizationsFromActiveCristinPersonAffiliations(affiliations, authorGroupWithCristinOrganization);
        var organizationsFromAuthorGroup = createOrganizationFromCristinOrganization(authorGroupWithCristinOrganization).toList();
        return cristinPersonActiveAffiliations.isEmpty()
                   ? organizationsFromAuthorGroup
                   : cristinPersonActiveAffiliations;
    }

    private static List<ImportOrganization> createOrganizationsFromActiveCristinPersonAffiliations(
        Set<Affiliation> affiliations, AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization) {
        var list = new ArrayList<ImportOrganization>();
        list.add(new ImportOrganization(null,
                                        AffiliationMapper.mapToAffiliation(getAffiliation(authorGroupWithCristinOrganization))));
        list.addAll(affiliations.stream()
                        .filter(Affiliation::isActive)
                        .map(CristinContributorExtractor::toOrganization)
                        .distinct()
                        .toList());
        return list;
    }

    private static AffiliationTp getAffiliation(
        AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization) {
        return Optional.ofNullable(authorGroupWithCristinOrganization).map(
            AuthorGroupWithCristinOrganization::getScopusAuthors).map(AuthorGroupTp::getAffiliation).orElse(null);
    }

    private static ImportOrganization toOrganization(Affiliation affiliation) {
        return new ImportOrganization(Organization.fromUri(affiliation.getOrganization()), ScopusAffiliation.emptyAffiliation());
    }

    private static ImportOrganization toOrganization(CristinOrganization cristinOrganization,
                                                  AuthorGroupTp authorGroupTp) {
        return new ImportOrganization(Organization.fromUri(cristinOrganization.id()),
                                      AffiliationMapper.mapToAffiliation(authorGroupTp.getAffiliation()));
    }

    private static Stream<ImportOrganization> createOrganizationFromCristinOrganization(
        AuthorGroupWithCristinOrganization authorGroupWithCristinOrganization) {
        return Optional.ofNullable(authorGroupWithCristinOrganization)
                   .map(AuthorGroupWithCristinOrganization::getCristinOrganizations)
                   .stream()
                   .flatMap(Collection::stream)
                   .distinct()
                   .map(cristinOrganization -> toOrganization(cristinOrganization,
                                                              authorGroupWithCristinOrganization.getScopusAuthors()));
    }

    private static int getSequenceNumber(AuthorTp authorTp) {
        return Integer.parseInt(authorTp.getSeq());
    }

    private static String determineContributorName(CristinPerson cristinPerson) {
        return constructName(cristinPerson);
    }

    private static String constructName(CristinPerson cristinPerson) {
        var firstName = getFirstName(cristinPerson);
        var lastName = getLastName(cristinPerson);
        if (firstName.isEmpty() && lastName.isEmpty()) {
            return StringUtils.EMPTY_STRING;
        }
        if (firstName.isEmpty() || lastName.isEmpty()) {
            return firstName + lastName;
        }
        return firstName + StringUtils.SPACE + lastName;
    }

    private static String getFirstName(CristinPerson cristinPerson) {
        return cristinPerson.getNames()
                   .stream()
                   .filter(CristinContributorExtractor::isFirstName)
                   .findFirst()
                   .map(TypedValue::getValue)
                   .orElse(StringUtils.EMPTY_STRING);
    }

    private static String getLastName(CristinPerson cristinPerson) {
        return cristinPerson.getNames()
                   .stream()
                   .filter(CristinContributorExtractor::isSurname)
                   .findFirst()
                   .map(TypedValue::getValue)
                   .orElse(StringUtils.EMPTY_STRING);
    }

    private static boolean isFirstName(TypedValue typedValue) {
        return FIRST_NAME_CRISTIN_FIELD_NAME.equals(getType(typedValue));
    }

    private static String getType(TypedValue typedValue) {
        return Optional.ofNullable(typedValue).map(TypedValue::getType).orElse(null);
    }

    private static boolean isSurname(TypedValue nameType) {
        return LAST_NAME_CRISTIN_FIELD_NAME.equals(getType(nameType));
    }

    private static boolean isCorrespondingAuthor(AuthorTp author, PersonalnameType correspondencePerson) {
        return getIndexedName(author).isPresent() && getIndexedName(author).get().equals(getIndexedName(correspondencePerson));
    }

    private static Optional<String> getIndexedName(AuthorTp author) {
        return Optional.ofNullable(author).map(AuthorTp::getIndexedName);
    }

    private static String getIndexedName(PersonalnameType correspondencePerson) {
        return Optional.ofNullable(correspondencePerson).map(PersonalnameType::getIndexedName).orElse(null);
    }

    private static boolean isOrcid(TypedValue identifier) {
        return ORCID_FIELD_NAME.equalsIgnoreCase(identifier.getType());
    }
}

