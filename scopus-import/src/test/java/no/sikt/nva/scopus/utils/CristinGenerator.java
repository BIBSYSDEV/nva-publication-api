package no.sikt.nva.scopus.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.core.attempt.Try.attempt;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.Role;
import no.sikt.nva.scopus.conversion.model.cristin.SearchOrganizationResponse;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import nva.commons.core.paths.UriWrapper;

public class CristinGenerator {

    private static final int MAX_NUMBER_OF_AFFILIATIONS = 10;

    public static CristinPerson generateCristinPerson(URI cristinId, String firstname, String surname) {
        var names = Set.of(new TypedValue("FirstName", firstname), new TypedValue("LastName", surname));
        return new CristinPerson.Builder().withId(cristinId)
                   .withNames(names)
                   .withAffiliations(generateAffiliations())
                   .withIdentifiers(Set.of(new TypedValue("orcid", randomString())))
                   .withVerifiedStatus(randomBoolean())
                   .build();
    }

    public static CristinPerson generateCristinPersonWithSingleActiveAffiliation(URI cristinId, String firstname,
                                                                                 String surname) {
        var names = Set.of(new TypedValue("FirstName", firstname), new TypedValue("LastName", surname));
        return new CristinPerson.Builder().withId(cristinId)
                   .withNames(names)
                   .withAffiliations(generateAffiliationsWithSingleActiveAffiliation())
                   .withIdentifiers(Set.of(new TypedValue("orcid", randomString())))
                   .withVerifiedStatus(randomBoolean())
                   .build();
    }

    public static CristinPerson generateCristinPersonWithoutAffiliations(URI cristinId, String firstname,
                                                                         String surname) {
        var names = Set.of(new TypedValue("FirstName", firstname), new TypedValue("LastName", surname));
        return new CristinPerson.Builder().withId(cristinId)
                   .withNames(names)
                   .withIdentifiers(Set.of(new TypedValue("orcid", randomString())))
                   .withVerifiedStatus(randomBoolean())
                   .build();
    }

    public static SearchOrganizationResponse generateSearchCristinOrganizationResponse(String organizationName) {
        var cristinOrganization = new CristinOrganization(randomUri(), randomUri(), randomString(), List.of(),
                                                          randomString(), Map.of(randomString(), organizationName));
        return new SearchOrganizationResponse(List.of(cristinOrganization), 1);
    }

    public static CristinOrganization generateOtherCristinOrganization(URI cristinId) {
        return new CristinOrganization(cristinId, randomUri(), randomString(), List.of(), "NO",
                                       Map.of(randomString(), "Andre institusjoner"));
    }

    public static CristinOrganization generateCristinOrganization(URI cristinId) {
        return new CristinOrganization(cristinId, randomUri(), randomString(), List.of(), randomString(),
                                       randomLabels());
    }

    public static CristinOrganization generateCristinOrganizationWithCountry(String country) {
        return new CristinOrganization(randomUri(), randomUri(), randomString(), List.of(), country, randomLabels());
    }

    public static String convertOrganizationToJson(CristinOrganization organization) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.writeValueAsString(organization);
    }

    public static String convertPersonToJson(CristinPerson cristinPerson) {
        return attempt(() -> JsonUtils.dtoObjectMapper.writeValueAsString(cristinPerson)).orElseThrow();
    }

    private static Set<Affiliation> generateAffiliations() {
        return IntStream.range(0, randomInteger(MAX_NUMBER_OF_AFFILIATIONS))
                   .mapToObj(index -> generateAffiliation())
                   .collect(Collectors.toSet());
    }

    private static Set<Affiliation> generateAffiliationsWithSingleActiveAffiliation() {
        return Set.of(generateInactiveAffiliation(), generateActiveAffiliation());
    }

    private static Affiliation generateAffiliation() {
        return new Affiliation(UriWrapper.fromUri(randomString()).getUri(), randomBoolean(), randomRole());
    }

    private static Affiliation generateActiveAffiliation() {
        return new Affiliation(UriWrapper.fromUri(randomString()).getUri(), true, randomRole());
    }

    private static Affiliation generateInactiveAffiliation() {
        return new Affiliation(UriWrapper.fromUri(randomString()).getUri(), false, randomRole());
    }

    private static Role randomRole() {
        return new Role(UriWrapper.fromUri(randomString()).getUri(), randomLabels());
    }

    private static Map<String, String> randomLabels() {
        var mapWithLabels = new HashMap<String, String>();
        mapWithLabels.put(randomString(), randomString());
        return mapWithLabels;
    }
}
