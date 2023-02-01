package no.sikt.nva.scopus.conversion;

import static java.util.Objects.nonNull;
import java.util.List;
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

    public static Contributor generateContributorFromCristin(Person person, AuthorTp authorTp,
                                                             PersonalnameType correspondencePerson) {
        return new Contributor(generateContributorIdentityFromCristinPerson(person),
                               generateOrganizationsFromCristinAffiliations(person.getAffiliations()), Role.CREATOR,
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

    private static List<Organization> generateOrganizationsFromCristinAffiliations(Set<Affiliation> affiliations) {
        return affiliations
                   .stream()
                   .map(affiliation ->
                            new Organization
                                    .Builder()
                                .withId(affiliation.getOrganization())
                                .withLabels(affiliation.getRole().getLabels())
                                .build())
                   .collect(Collectors.toList());
    }

    private static int getSequenceNumber(AuthorTp authorTp) {
        return Integer.parseInt(authorTp.getSeq());
    }

    private static String determineContributorName(Person person) {
        return getLastName(person)
               + NAME_DELIMITER
               + getFirstName(person);
    }

    @NotNull
    private static String getFirstName(Person person) {
        return person.getNames().stream()
                   .filter(CristinContributorExtractor::isFirstName).findFirst().map(
                TypedValue::getValue).orElse(StringUtils.EMPTY_STRING);
    }

    @NotNull
    private static String getLastName(Person person) {
        return person.getNames().stream().filter(CristinContributorExtractor::isSurname).findFirst()
                   .map(TypedValue::getValue).orElse(StringUtils.EMPTY_STRING);
    }

    private static boolean isFirstName(TypedValue typedValue) {
        return nonNull(typedValue) && FIRST_NAME_CRISTIN_FIELD_NAME.equals(typedValue.getType());
    }

    private static boolean isSurname(TypedValue nameType) {
        return nonNull(nameType) && LAST_NAME_CRISTIN_FIELD_NAME.equals(nameType.getType());
    }

    private static boolean isCorrespondingAuthor(AuthorTp author, PersonalnameType correspondencePerson) {
        return nonNull(correspondencePerson)
               && author.getIndexedName().equals(correspondencePerson.getIndexedName());
    }

    private static boolean isOrcid(TypedValue identifier) {
        return ORCID_FIELD_NAME.equalsIgnoreCase(identifier.getType());
    }
}

