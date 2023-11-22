package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.conversion.model.NvaCustomerContributor;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import no.unit.nva.model.ContributorVerificationStatus;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.role.Role;
import no.unit.nva.model.role.RoleType;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;

public final class CristinContributorExtractor {

    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final String ORCID_FIELD_NAME = "orcid";

    @JacocoGenerated
    private CristinContributorExtractor() {
    }

    public static NvaCustomerContributor generateContributorFromCristinPerson(CristinPerson cristinPerson, AuthorTp authorTp,
                                                                   PersonalnameType correspondencePerson,
                                                                   CristinOrganization cristinOrganization,
                                                                   boolean isNvaCustomer) {

        return new NvaCustomerContributor.Builder().withIdentity(generateContributorIdentityFromCristinPerson(cristinPerson))
                   .withAffiliations(generateOrganizations(cristinPerson.getAffiliations(), cristinOrganization))
                   .withRole(new RoleType(Role.CREATOR))
                   .withSequence(getSequenceNumber(authorTp))
                   .withCorrespondingAuthor(isCorrespondingAuthor(authorTp, correspondencePerson))
                   .withBelongsToNvaCustomer(isNvaCustomer)
                   .build();
    }

    private static Identity generateContributorIdentityFromCristinPerson(CristinPerson cristinPerson) {
        var identity = new Identity();
        identity.setName(determineContributorName(cristinPerson));
        identity.setOrcId(cristinPerson.getIdentifiers()
                              .stream()
                              .filter(CristinContributorExtractor::isOrcid)
                              .findAny()
                              .map(TypedValue::getValue)
                              .orElse(null));
        identity.setId(cristinPerson.getId());
        identity.setVerificationStatus(nonNull(cristinPerson.getVerified()) ? generateVerificationStatus(cristinPerson)
                                           : ContributorVerificationStatus.CANNOT_BE_ESTABLISHED);
        return identity;
    }

    private static ContributorVerificationStatus generateVerificationStatus(CristinPerson cristinPerson) {
        return Boolean.TRUE.equals(cristinPerson.getVerified())
                   ? ContributorVerificationStatus.VERIFIED
                   : ContributorVerificationStatus.NOT_VERIFIED;
    }

    private static List<Organization> generateOrganizations(Set<Affiliation> affiliations,
                                                            CristinOrganization cristinOrganization) {

        var organizations = createOrganizationsFromCristinPersonAffiliations(affiliations).toList();
        var organisationFromAuthorGroupTp = createOrganizationFromCristinOrganization(cristinOrganization).toList();
        return Stream.concat(organizations.stream(), organisationFromAuthorGroupTp.stream())
                   .filter(Objects::nonNull)
                   .toList();
    }

    private static Stream<Organization> createOrganizationsFromCristinPersonAffiliations(
        Set<Affiliation> affiliations) {
        return affiliations.stream()
                   .filter(Affiliation::isActive)
                   .map(CristinContributorExtractor::convertToOrganization);
    }

    private static Organization convertToOrganization(Affiliation affiliation) {
        return new Organization.Builder().withId(affiliation.getOrganization())
                   .withLabels(affiliation.getRole().getLabels())
                   .build();
    }

    private static Stream<Organization> createOrganizationFromCristinOrganization(
        CristinOrganization cristinOrganization) {
        return Stream.ofNullable(cristinOrganization).map(CristinContributorExtractor::toOrganization);
    }

    private static Organization toOrganization(CristinOrganization cristinOrganization) {
        return new Organization.Builder().withId(cristinOrganization.id())
                   .withLabels(cristinOrganization.labels())
                   .build();
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
        return author.getIndexedName().equals(getIndexedName(correspondencePerson));
    }

    private static String getIndexedName(PersonalnameType correspondencePerson) {
        return Optional.ofNullable(correspondencePerson).map(PersonalnameType::getIndexedName).orElse(null);
    }

    private static boolean isOrcid(TypedValue identifier) {
        return ORCID_FIELD_NAME.equalsIgnoreCase(identifier.getType());
    }
}

