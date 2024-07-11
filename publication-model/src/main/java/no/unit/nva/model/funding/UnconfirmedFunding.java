package no.unit.nva.model.funding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UnconfirmedFunding implements Funding {
    private final URI source;
    private final String identifier;
    private final Map<String, String> labels;
    private final MonetaryAmount fundingAmount;
    private final Instant activeFrom;
    private final Instant activeTo;

    @JsonCreator
    protected UnconfirmedFunding(@JsonProperty("source") URI source,
                                 @JsonProperty("identifier") String identifier,
                                 @JsonProperty("labels") Map<String, String> labels,
                                 @JsonProperty("fundingAmount") MonetaryAmount fundingAmount,
                                 @JsonProperty("activeFrom") Instant activeFrom,
                                 @JsonProperty("activeTo") Instant activeTo) {
        this.source = source;
        this.identifier = identifier;
        this.labels = labels;
        this.fundingAmount = fundingAmount;
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
    }

    @Override
    public URI getSource() {
        return source;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public Map<String, String> getLabels() {
        return labels;
    }

    @Override
    public MonetaryAmount getFundingAmount() {
        return fundingAmount;
    }

    @Override
    public Instant getActiveFrom() {
        return activeFrom;
    }

    @Override
    public Instant getActiveTo() {
        return activeTo;
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
        UnconfirmedFunding unconfirmedFunding = (UnconfirmedFunding) o;
        return source.equals(unconfirmedFunding.source)
                && Objects.equals(identifier, unconfirmedFunding.identifier)
                && Objects.equals(labels, unconfirmedFunding.labels)
                && Objects.equals(fundingAmount, unconfirmedFunding.fundingAmount)
                && Objects.equals(activeFrom, unconfirmedFunding.activeFrom)
                && Objects.equals(activeTo, unconfirmedFunding.activeTo);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(source, identifier, labels, fundingAmount, activeFrom, activeTo);
    }

}
