package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.scopus.generated.AuthorTp;
import no.scopus.generated.PersonalnameType;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.Person;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;
import no.unit.nva.model.Contributor;
import no.unit.nva.model.Identity;
import no.unit.nva.model.Organization;
import no.unit.nva.model.Role;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import org.jetbrains.annotations.NotNull;

public final class CristinContributorExtractor {

    public static final String NAME_DELIMITER = ", ";
    public static final String FIRST_NAME_CRISTIN_FIELD_NAME = "FirstName";
    public static final String LAST_NAME_CRISTIN_FIELD_NAME = "LastName";
    public static final String ORCID_FIELD_NAME = "orcid";

    @JacocoGenerated
    private CristinContributorExtractor() {
    }

    public static Contributor generateContributorFromCristin(
        Person person,
        AuthorTp authorTp,
        PersonalnameType correspondencePerson,
        no.sikt.nva.scopus.conversion.model.cristin.Organization organization) {

        return new Contributor(generateContributorIdentityFromCristinPerson(person),
                               generateOrganizationsFromCristinAffiliations(person.getAffiliations(), organization),
                               Role.CREATOR,
                               getSequenceNumber(authorTp), isCorrespondingAuthor(authorTp, correspondencePerson));
    }

    private static Identity generateContributorIdentityFromCristinPerson(Person cristinPerson) {
        var identity = new Identity();
        identity.setName(determineContributorName(cristinPerson));
        identity.setOrcId(cristinPerson
                              .getIdentifiers()
                              .stream()
                              .filter(CristinContributorExtractor::isOrcid)
                              .findAny().map(TypedValue::getValue)
                              .orElse(null));
        identity.setId(cristinPerson.getId());
        return identity;
    }

    private static List<Organization> generateOrganizationsFromCristinAffiliations(
        Set<Affiliation> affiliations,
        no.sikt.nva.scopus.conversion.model.cristin.Organization organization) {

        var organizations = createOrganizationsFromCristinPersonAffiliations(affiliations);
        if (nonNull(organization)) {
            organizations.add(createOrganizationFromAuthorGroupTpAffiliation(organization));
            return organizations;
        }
        return organizations;
    }

    @NotNull
    private static List<Organization> createOrganizationsFromCristinPersonAffiliations(Set<Affiliation> affiliations) {
        return affiliations.stream()
                   .map(CristinContributorExtractor::convertToOrganization)
                   .collect(Collectors.toList());
    }

    @NotNull
    private static Organization convertToOrganization(Affiliation affiliation) {
        return new Organization.Builder()
                   .withId(affiliation.getOrganization())
                   .withLabels(affiliation.getRole().getLabels())
                   .build();
    }

    private static Organization createOrganizationFromAuthorGroupTpAffiliation(
        no.sikt.nva.scopus.conversion.model.cristin.Organization organization) {
        return Optional.ofNullable(organization)
                   .map(CristinContributorExtractor::toOrganization)
                   .orElse(null);
    }

    private static Organization toOrganization(no.sikt.nva.scopus.conversion.model.cristin.Organization organization) {
        return new Organization.Builder()
                   .withId(organization.getId())
                   .withLabels(organization.getLabels())
                   .build();
    }

    private static int getSequenceNumber(AuthorTp authorTp) {
        return Integer.parseInt(authorTp.getSeq());
    }

    private static String determineContributorName(Person person) {
        return constructName(person);
    }

    private static String constructName(Person person) {
        var firstName = getFirstName(person);
        var lastName = getLastName(person);
        if (firstName.isEmpty() && lastName.isEmpty()) {
            return StringUtils.EMPTY_STRING;
        }
        if (firstName.isEmpty() || lastName.isEmpty()) {
            return firstName + lastName;
        }
        return lastName + NAME_DELIMITER + firstName;
    }

    private static String getFirstName(Person person) {
        return person.getNames().stream()
                   .filter(CristinContributorExtractor::isFirstName).findFirst().map(
                TypedValue::getValue).orElse(StringUtils.EMPTY_STRING);
    }

    private static String getLastName(Person person) {
        return person.getNames().stream().filter(CristinContributorExtractor::isSurname).findFirst()
                   .map(TypedValue::getValue).orElse(StringUtils.EMPTY_STRING);
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

