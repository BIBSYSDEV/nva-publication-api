package no.sikt.nva.scopus.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomBoolean;
import static no.unit.nva.testutils.RandomDataGenerator.randomInteger;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import no.sikt.nva.scopus.conversion.model.cristin.Affiliation;
import no.sikt.nva.scopus.conversion.model.cristin.CristinOrganization;
import no.sikt.nva.scopus.conversion.model.cristin.CristinPerson;
import no.sikt.nva.scopus.conversion.model.cristin.Role;
import no.sikt.nva.scopus.conversion.model.cristin.TypedValue;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.paths.UriWrapper;

public class CristinGenerator {

    private static final int MAX_NUMBER_OF_AFFILIATIONS = 10;

    public static CristinPerson generateCristinPerson(URI cristinId, String firstname, String surname) {
        var names = Set.of(new TypedValue("FirstName", firstname), new TypedValue("LastName", surname));
        return new CristinPerson.Builder()
                   .withId(cristinId)
                   .withNames(names).withAffiliations(generateAffiliations())
                   .withIdentifiers(Set.of(new TypedValue("orcid", randomString())))
                   .withVerifiedStatus(randomBoolean())
                   .build();
    }

    public static CristinPerson generateCristinPersonWithoutAffiliations(URI cristinId, String firstname,
                                                                       String surname) {
        var names = Set.of(new TypedValue("FirstName", firstname), new TypedValue("LastName", surname));
        return new CristinPerson.Builder()
                   .withId(cristinId)
                   .withNames(names)
                   .withIdentifiers(Set.of(new TypedValue("orcid", randomString())))
                   .withVerifiedStatus(randomBoolean())
                   .build();
    }

    public static CristinOrganization generateOtherCristinOrganization(URI cristinId) {
        return new CristinOrganization(cristinId, Map.of(randomString(), "Andre institusjoner"), "NO");
    }

    public static CristinOrganization generateCristinOrganization(URI cristinId) {
        return new CristinOrganization(cristinId, randomLabels(), randomString());
    }

    public static CristinOrganization generateCristinOrganizationWithCountry(String country) {
        return new CristinOrganization(randomUri(), randomLabels(), country);
    }

    public static String convertPersonToJson(CristinPerson cristinPerson) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.writeValueAsString(cristinPerson);
    }

    public static String convertOrganizationToJson(CristinOrganization cristinOrganization) throws JsonProcessingException {
        return JsonUtils.dtoObjectMapper.writeValueAsString(cristinOrganization);
    }

    private static Set<Affiliation> generateAffiliations() {
        return IntStream.range(0, randomInteger(MAX_NUMBER_OF_AFFILIATIONS))
                   .mapToObj(index -> generateAffiliation())
                   .collect(Collectors.toSet());
    }

    private static Affiliation generateAffiliation() {
        return new Affiliation(UriWrapper.fromUri(randomString()).getUri(), randomBoolean(), randomRole());
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
