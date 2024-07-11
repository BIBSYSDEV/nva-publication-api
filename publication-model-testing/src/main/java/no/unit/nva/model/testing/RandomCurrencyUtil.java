package no.unit.nva.model.testing;

import no.unit.nva.model.Currency;
import no.unit.nva.testutils.RandomDataGenerator;

public final class RandomCurrencyUtil {
    private RandomCurrencyUtil() {
        // no-op
    }

    public static Currency randomCurrency() {
        return RandomDataGenerator.randomElement(Currency.values());
    }

}
