package no.sikt.nva.scopus.conversion;

import static no.sikt.nva.scopus.conversion.AffiliationMapper.mapToAffiliation;
import static no.sikt.nva.scopus.utils.ScopusGenerator.randomAffiliation;
import static no.sikt.nva.scopus.utils.ScopusGenerator.randomCollaboration;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import no.scopus.generated.AffiliationTp;
import no.scopus.generated.CollaborationTp;
import no.scopus.generated.OrganizationTp;
import no.unit.nva.importcandidate.ScopusAffiliation;
import org.junit.jupiter.api.Test;

class ScopusAffiliationMapperTest {

    @Test
    void shouldMapNullToEmptyAffiliation() {
        assertEquals(ScopusAffiliation.emptyAffiliation(), mapToAffiliation((AffiliationTp) null));
    }

    @Test
    void shouldMapAffiliationId() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        assertEquals(scopusAffiliation.getAfid(), affiliation.identifier().affiliation());
    }

    @Test
    void shouldMapDepartmentId() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        assertEquals(scopusAffiliation.getDptid(), affiliation.identifier().department());
    }

    @Test
    void shouldMapCountryCode() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        assertEquals(scopusAffiliation.getCountry(), affiliation.country().code());
    }

    @Test
    void shouldMapCountryName() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        assertEquals(scopusAffiliation.getCountryAttribute(), affiliation.country().name());
    }

    @Test
    void shouldMapOrganizationNames() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        var expectedOrganizationNames = createExpectedOrganizationNames(scopusAffiliation);

        assertThat(expectedOrganizationNames, containsInAnyOrder(affiliation.names().toArray()));
    }

    @Test
    void shouldMapCity() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        assertEquals(scopusAffiliation.getCity(), affiliation.address().city());
    }

    @Test
    void shouldMapCityGroup() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        assertEquals(scopusAffiliation.getCityGroup(), affiliation.address().locality());
    }

    @Test
    void shouldMapAddressPart() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        var expectedAddressPart = extractAddressPart(scopusAffiliation);

        assertEquals(expectedAddressPart, affiliation.address().street());
    }

    @Test
    void shouldMapPostalCode() {
        var scopusAffiliation = randomAffiliation();

        var affiliation = mapToAffiliation(scopusAffiliation);

        var expectedPostalCode = extractPostalCode(scopusAffiliation);

        assertEquals(expectedPostalCode, affiliation.address().postalCode());
    }

    @Test
    void shouldMapTextNameWhenCollaboration() {
        var scopusAffiliation = randomCollaboration();

        var affiliation = mapToAffiliation(scopusAffiliation);

        var expectedNames = getExpectedNames(scopusAffiliation);

        assertThat(expectedNames, containsInAnyOrder(affiliation.names().toArray()));
    }

    private List<String> getExpectedNames(CollaborationTp collaborationTp) {
        var names = new ArrayList<>(collaborationTp.getText().getContent().stream().map(String::valueOf).toList());
        names.add(collaborationTp.getIndexedName());
        return names;
    }

    private static List<String> createExpectedOrganizationNames(AffiliationTp scopusAffiliation) {
        return scopusAffiliation.getOrganization()
                   .stream()
                   .map(OrganizationTp::getContent)
                   .flatMap(List::stream)
                   .map(String::valueOf)
                   .toList();
    }

    private static String extractAddressPart(AffiliationTp scopusAffiliation) {
        return scopusAffiliation.getAddressPart().getContent().getFirst().toString();
    }

    private static String extractPostalCode(AffiliationTp affiliationTp) {
        return affiliationTp.getPostalCode().getFirst().getContent();
    }
}