package no.unit.nva.model.funding;


import java.net.URI;
import java.time.Instant;
import java.util.Map;

import static java.util.Objects.nonNull;

public final class FundingBuilder {
    private URI source;
    private URI id;
    private String identifier;
    private Map<String, String> labels;
    private MonetaryAmount fundingAmount;
    private Instant activeFrom;
    private Instant activeTo;

    public FundingBuilder() {
    }

    public FundingBuilder withSource(URI source) {
        this.source = source;
        return this;
    }

    public FundingBuilder withId(URI id) {
        this.id = id;
        return this;
    }

    public FundingBuilder withIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public FundingBuilder withLabels(Map<String, String> labels) {
        this.labels = labels;
        return this;
    }

    public FundingBuilder withFundingAmount(MonetaryAmount fundingAmount) {
        this.fundingAmount = fundingAmount;
        return this;
    }

    public FundingBuilder withActiveFrom(Instant activeFrom) {
        this.activeFrom = activeFrom;
        return this;
    }

    public FundingBuilder withActiveTo(Instant activeTo) {
        this.activeTo = activeTo;
        return this;
    }

    public Funding build() {
        return nonNull(id)
                ? new ConfirmedFunding(source, id, identifier, labels, fundingAmount, activeFrom, activeTo)
                : new UnconfirmedFunding(source, identifier, labels, fundingAmount, activeFrom, activeTo);
    }
}