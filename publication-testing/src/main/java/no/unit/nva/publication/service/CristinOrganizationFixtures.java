package no.unit.nva.publication.service;

import static no.unit.nva.publication.uriretriever.FakeUriResponse.API_HOST;

import net.datafaker.Faker;

import nva.commons.core.paths.UriWrapper;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

public final class CristinOrganizationFixtures {
    private static final String COUNTRY_CODE_NORWAY = "NO";
    private static final Faker FAKER = new Faker();

    private CristinOrganizationFixtures() {}

    public static URI organizationIdFromIdentifier(String identifier) {
        return UriWrapper.fromHost(API_HOST)
                .addChild("cristin")
                .addChild("organization")
                .addChild(identifier)
                .getUri();
    }

    public static String randomAcademicUnit() {
        var baseUnits = List.of("University of", "Department of", "Institute of", "Section for");
        var base = FAKER.options().option(baseUnits.toArray(String[]::new));
        return String.join(" ", base, FAKER.word().adjective(), FAKER.word().noun());
    }

    public static URI randomOrganizationId() {
        var identifier = FAKER.numerify("###.###.###.###");
        return organizationIdFromIdentifier(identifier);
    }

    public static FakeCristinOrganization.Builder randomCristinOrganization(URI organizationId) {
        var label = randomAcademicUnit();
        return FakeCristinOrganization.builder()
                .withId(organizationId)
                .withAcronym(FAKER.word().noun().toUpperCase(Locale.ROOT))
                .withCountryCode(COUNTRY_CODE_NORWAY)
                .withContext("https://bibsysdev.github.io/src/organization-context.json")
                .withType("Organization")
                .withLabels(Map.of("nb", label, "en", label));
    }

    public static FakeCristinOrganization.Builder randomCristinOrganization(
            URI organizationId, int numberOfSubOrganizations) {
        var selfReferentialLeafNode = FakeCristinOrganization.asLeafNode(organizationId);
        var subOrganizations =
                IntStream.range(0, numberOfSubOrganizations)
                        .mapToObj(
                                i ->
                                        randomCristinOrganization(randomOrganizationId())
                                                .withPartOf(List.of(selfReferentialLeafNode)))
                        .map(FakeCristinOrganization.Builder::build)
                        .toList();

        return randomCristinOrganization(organizationId).withHasPart(subOrganizations);
    }
}
