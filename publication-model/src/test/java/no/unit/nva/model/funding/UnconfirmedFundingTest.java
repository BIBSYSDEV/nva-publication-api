package no.unit.nva.model.funding;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UnconfirmedFundingTest {

    @Test
    void shouldNotFailWhenComparingUnconfirmedFundingWithNullSource() {
        var unconfirmedFunding = new UnconfirmedFunding(null, randomString(), null, null, null, null);
        var anotherUnconfirmedFunding = new UnconfirmedFunding(randomUri(), randomString(), Map.of(), null, null, null);

        assertDoesNotThrow(() -> unconfirmedFunding.equals(anotherUnconfirmedFunding));
        assertDoesNotThrow(() -> anotherUnconfirmedFunding.equals(unconfirmedFunding));
    }
}