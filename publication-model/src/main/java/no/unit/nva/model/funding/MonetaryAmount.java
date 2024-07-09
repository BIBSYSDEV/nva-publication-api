package no.unit.nva.model.funding;

import no.unit.nva.model.Currency;

import java.util.Objects;
import nva.commons.core.JacocoGenerated;

public class MonetaryAmount {
    private Currency currency;
    private long amount;

    public MonetaryAmount() {
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MonetaryAmount that = (MonetaryAmount) o;
        return amount == that.amount && currency == that.currency;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(currency, amount);
    }
}
