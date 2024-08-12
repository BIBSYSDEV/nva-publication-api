package no.sikt.nva.scopus.conversion;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import no.unit.nva.expansion.model.cristin.CristinOrganization;
import org.junit.jupiter.api.Test;

public class CristinOrganizationTest {

    @Test
    void shouldReturnTopLevelOrgWhenCristinOrgTopLevelOrg() {
        var org = new CristinOrganization(randomUri(), null, null, List.of(), null, Map.of());

        assertThat(org.id(), is(equalTo(org.getTopLevelOrg().id())));
    }

    @Test
    void shouldReturnTrueWhenOrganizationContainsLabelWithValue() {
        var value = randomString();
        var cristinOrganization = new CristinOrganization(randomUri(), null, null, List.of(),
                                                          null, Map.of(randomString(), value));

        assertTrue(cristinOrganization.containsLabelWithValue(value));
    }
}
