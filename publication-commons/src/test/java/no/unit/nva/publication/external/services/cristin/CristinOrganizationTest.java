package no.unit.nva.publication.external.services.cristin;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CristinOrganizationTest {

    @Test
    void shouldReturnTrueWhenOrganizationContainsLabelWithValue() {
        var value = randomString();
        var cristinOrganization = new CristinOrganization(randomUri(), null, null, List.of(), null,
                                                          Map.of(randomString(), value));

        assertTrue(cristinOrganization.containsLabelWithValue(value));
    }

    @Test
    void shouldReturnFalseWhenOrganizationLabelsAreEmpty() {
        var value = randomString();
        var cristinOrganization = new CristinOrganization(randomUri(), null, null, List.of(), null,
                                                          Map.of());

        assertFalse(cristinOrganization.containsLabelWithValue(value));
    }

    @Test
    void shouldReturnFalseWhenOrganizationLabelsAreNulls() {
        var value = randomString();
        var cristinOrganization = new CristinOrganization(randomUri(), null, null, List.of(), null,
                                                          null);

        assertFalse(cristinOrganization.containsLabelWithValue(value));
    }
}